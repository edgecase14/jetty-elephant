/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.security.auth.UserPrincipal;
import java.security.Principal;
import java.util.concurrent.BlockingQueue;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.server.Response;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.coplanar.ents.TsUser;
import net.coplanar.updatemsg.UpdateMessage;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.Subject;
import net.coplanar.updatemsg.SetController;
import org.eclipse.jetty.util.Callback;

/**
 *
 * @author jjackson
 */
public class GenericThread implements Runnable {

    private BlockingQueue<UpdateMessage> myQueue;
    private Session ses;
    List<UpdateMessage> ul;
    org.hibernate.Session hsession;
    private Response resp;
    private Subject subj;
    String username;
    private GenericController ctlr = null;

    // would be nice if there was a way for subclasses to not have to include a constructor
    //GenericThread (BlockingQueue<UpdateMessage> myq, Session ses) {
    //    this.myQueue = myq;
    //    this.ses = ses;
    //}
    final public void setMyQueueSes(BlockingQueue<UpdateMessage> myq, Session ses, Subject subject) {
        this.myQueue = myq;
        this.ses = ses;
        this.subj = subject;
    }

    final void addResponseObj(UpdateMessage msg) {
        this.ul.add(msg);
    }

    final void writeObjList() throws JsonProcessingException {
        // no callback is suspicious - what keeps multiple writes in sequence?  maybe use synchronous version?
        SseHandler.writeObjList(this.resp, this.ul, Callback.NOOP);
        // we still hold a reference to ul, so it (incorrectly) runs concurrently with SseHandler
        // so clear it now
        ul = new ArrayList<>();

    }
    
    public final GenericController getController() {
        // concurrency issues?  called by handler
        return ctlr;
    }
    
    public final BlockingQueue<UpdateMessage> getQueue() {
        return myQueue;
    }

    @Override
    public final void run() {
        try {
            System.out.println("new GenericThread.");

            var princ = subj.getPrincipals();
            for (Principal p : princ) {
                // last one wins?
                username = p.getName();
            }
            if (username == null) {
                // problem
            }
            Thread.currentThread().setName("GenericThread-" + username);

            try {
                var ctx = new InitialDirContext(SrvApp.ldap_env);

                // Fire off an LDAP user groups lookup here, and add to Subject
                // would msUPN have the email part or full ad.blah domain?
                String searchFilter = "(&(objectClass=user)(sAMAccountName=" + username.split("@")[0] + "))";
                String searchBase = "dc=ad,dc=coplanar,dc=net";
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                // make results less verbose
                searchControls.setReturningAttributes(new String[]{"dn", "memberOf"});

                //System.out.println("GenericThread LDAP lookup: " + username);
                NamingEnumeration<SearchResult> results = ctx.search(searchBase, searchFilter, searchControls);

                if (!results.hasMoreElements()) {
                    System.out.println("User not found.");
                    // bail out
                }

                SearchResult searchResult = (SearchResult) results.nextElement();
                if (results.hasMoreElements()) {
                    System.out.println("Search result contains ambiguous entries");
                    // bail out
                }

                String user_dn = null;
                //while (results.hasMore()) {
                //SearchResult searchResult = results.next();
                Attributes attrs = searchResult.getAttributes();

                // Print all attributes
                NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();
                while (allAttrs.hasMore()) {
                    Attribute attr = allAttrs.next();
                    System.out.println(attr.getID() + ": ");

                    if (attr.getID().equals("memberOf")) {
                        NamingEnumeration<?> allVals = attr.getAll();
                        while (allVals.hasMore()) {
                            String val = (String) allVals.next();
                            //System.out.println(val);
                            // mapping function?
                            princ.add(new UserPrincipal(val));
                        }
                    }

                }
                //}

                ctx.close();
            } catch (NamingException ex) {
                Logger.getLogger(GenericThread.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println("Subject with groups: " + subj);

            hsession = SrvApp.sf.openSession();

            // should lookup authenticated user's username - get by naturalId
            TsUser tu = hsession.bySimpleNaturalId(TsUser.class).load(username);
            String user = "NONE";
            if (tu != null) {
                user = tu.getEmail();
            } // else bail out

            System.out.println(
                    "GenericThread tsuser:" + user);

            SetController threadType = null;

            System.out.println(
                    "GenericThread controller Queue " + myQueue);
            while (!Thread.currentThread()
                    .isInterrupted()) {
                // could we do Hibernate LAZY load prefetch in background by doing work from a (micro) task queue here?
                // maybe prioritize the tasks from frontend UI messages first though.
                try {
                    UpdateMessage event = myQueue.take();
                    
                    if (event.getClass() == SetController.class) {
                        if (ctlr != null) {
                            // TODO - reset session/thread for new controller
                            // simple as ctlr.reset() ?
                            System.out.println("UNSUPPORTED: change controller.");
                        }
                        threadType = (SetController) event;
                        ctlr = threadType.controller;

                        ctlr.setHsession(hsession);
                        ctlr.setUser(tu);

                        ctlr.setParentThread(
                                this);

                        System.out.println(
                                "GenericThread controller set. ");

                        ctlr.init();  // 1 time setup
                        continue;
                    }

                    if (ctlr == null) 
                        continue;
                    
                    // races with EventSource reconnects
                    resp = (Response) ses.getAttribute("sse");
                    if (resp == null) {
                        System.out.println("error: null sse - refresh browser");
                        // FIXME totally bail, browser state now stalled
                        continue;
                    }

                    ul = new ArrayList<>();
                    ctlr.controller(event);
                } catch (InterruptedException e) {
                    System.out.println("interrupt GenericThread.");
                    Thread.currentThread().interrupt();
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(TscController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
            // cleanup - remove eventQueue from Session

            System.out.println("exit GenericThread.");

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            SrvApp.as.removeThread(ses.getId());
        }
    }
}