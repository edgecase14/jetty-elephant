/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import net.coplanar.updatemsg.NewPeriod;
import net.coplanar.updatemsg.SetController;
import net.coplanar.updatemsg.UpdateMessage;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

/**
 *
 * @author jjackson
 */
public class ThreadTypeHandler extends Handler.Wrapper {

    private AppSessions appSessions;

    // TODO - no-arg constructor required? Jetty bug maybe, there's a no-arg constructor one of the superclasses that doesn't make sense
    
    public ThreadTypeHandler(boolean dynamic, Handler handler) {
        super( dynamic, handler);
        //this.sessionThreads = new HashMap<>();
    }
    
    public ThreadTypeHandler(Handler handler) {
        this(false, handler);
    }
    
    public void setAppSession(AppSessions as) {
        appSessions = as;
    }

    // comes after Session created, and Listener has started GenericThread - we pass it a GenericController to use thru it's eventQueue
    // hopefully HTTP Cache control header : must-revalidate will cause the controller-name.html to always be hit on browser JS VM (re)start
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
    
        // if session exists, pass to next handler, if not, create one to fire a user lookup async to hide latency
        Session ses = request.getSession(false); // F1
        // TODO - filter to only handle URL path /ctlr/
        if (ses != null) {
            String sid = ses.getId();
            //String user = (String) request.getAttribute("user");
            System.out.println("\nrequest session id: " + sid );  //+ " user: " + user);
            var st = appSessions.getThread(sid);
            if (st == null) {
                System.out.println("no thread for session!");
            }

            // if URI = index.html, set controller type and specific query strings
            String path = request.getHttpURI().getDecodedPath();
            System.out.println("PATH: " + path);
            
            
            // query string parsing should be folded into GenericController
            var query = request.getHttpURI().getQuery();
            MultiMap<String> queryMap =  queryMap = new MultiMap<>();
            if (query != null) {
                UrlEncoded.decodeTo(query, queryMap, StandardCharsets.UTF_8);
            }

            // register these paths using annotations on GenericController implementations?
            // then call the annotated function on match
            if ("/timesheet.html".equals(path) ) { //|| "/tsc2/".equals(path)) { // or /tsc2/timesheet
                Class<?> ctl_class = TscController.class;
                
                //var eq = (BlockingQueue<UpdateMessage>) ses.getAttribute("initialQueue");
                GenericThread gt = SrvApp.as.getThread(sid);
                BlockingQueue<UpdateMessage> eq = gt.getQueue();
                GenericController ctlr = gt.getController();
                if (ctlr == null) {
                    var setCtlr = new SetController();
                    // class should be based on the URL path - /ctlr/tsc2 = TscController
                    // generate this mapping using an Annotation?
                    var tsc = (GenericController) ctl_class.getDeclaredConstructor().newInstance();
                    ((TscController) tsc).querySetup(queryMap);  // can this be merged with queryNavigated()?
                    //if (user != null) {
                    //    setCtlr.user = user; // let controller do Hibernate find(TsUser.class) since we can't block in handler
                    //}
                    setCtlr.controller = (GenericController) tsc;
                    System.out.println("setting controller to TscController" + eq);
                    var ret = eq.offer(setCtlr);
                    System.out.println("setting controller to TscController done." + ret);
                } else {
                    System.out.println("found thread, but has controller!");
                    // if same controller already, the rest is ok?
                    if (ctlr.getClass().equals(ctl_class)) {
                        System.out.println("not init, found eventQueue");
                        UpdateMessage um = TscController.queryNavigated(queryMap);  // trigger browser reload()
                        if (um != null) {
                            eq.offer(um);
                        }
                        //if (pay_period != null) { // and is different?
                        //    var npp = new NewPeriod();
                        //    npp.pay_period = pay_period;
                        //    eq2.offer(npp);
                        //}
                    } else {
                        // change controller not supported yet
                    }
                }
                
            } else {
                //System.out.println("not a known controller");
            }
        } else {
            System.out.println("request without session!");
        }
        return super.handle(request, response, callback);
    }
    
}
