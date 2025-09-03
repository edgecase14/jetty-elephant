/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

/**
 *
 * @author jjackson
 */
import com.sun.security.auth.UserPrincipal;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import net.coplanar.updatemsg.UpdateMessage;
import org.apache.kerby.kerberos.kerb.KrbCodec;
import org.apache.kerby.kerberos.kerb.type.base.KrbToken;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import org.eclipse.jetty.io.EndPoint.SslSessionData;

public class KerberosHandler extends Handler.Wrapper {
    
    private AppSessions appSessions;
    private final ExecutorService vexec;

    public KerberosHandler(boolean dynamic, Handler handler) {
        super(dynamic, handler);
/*
        ThreadFactory threadFactory = Thread.ofPlatform()
                .name("GenericThread-", 0)
                .factory();
        this.vexec = Executors.newThreadPerTaskExecutor(threadFactory);
*/
        // jvm arg: -Djdk.tracePinnedThreads=full to check for synchronized() issues, which might exist in 3rd party libraries
        this.vexec = Executors.newVirtualThreadPerTaskExecutor();
    }

    public KerberosHandler(Handler handler) {
        this(false, handler);
    }
    
    public void setAppSession(AppSessions as) {
        appSessions = as;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback)
            throws Exception {

        if (request.getSession(false) != null) {
            // lets hope this is secure
            return super.handle(request, response, callback);
        }
        
        boolean authSucceeded = false;
        String username = null;

        // Check if the request is already authenticated via TLS
//        var ssd = (SslSessionData) request.getAttribute("EndPoint.SslSessionData.ATTRIBUTE");
        var ssd = (SslSessionData) request.getAttribute("org.eclipse.jetty.io.Endpoint.SslSessionData");
        X509Certificate[] certs = null;
        if (ssd != null) {
            certs = (X509Certificate[]) ssd.peerCertificates();
        } else {
            System.out.println("TLS session data null: " );
        }
//        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null && certs.length > 0) {
            // TLS authentication succeeded, skip Kerberos authentication
            var certuser = certs[0].getSubjectX500Principal().getName();
            // java.util.Collection<List<?>> certuser2 = certs[0].getSubjectAlternativeNames();
            // now pull out the msUPN or email address
            System.out.println("certuser: " + certuser);
            // TODO - mapping function?
            username = certuser;
            request.setAttribute("user", certuser);
            var ses_for_cert = request.getSession(true);
            ses_for_cert.setAttribute("username", username);
            // how about create a new Subject(), and add a Principal?
            // LDAP login module can be run on vthread, let ldap client block all it wants
            authSucceeded = true;
        } else {
            System.out.println("no peer certificates.");
        }

        if (!authSucceeded) { // and kerberos is setup properly - maybe check that keytab is valid?
            // Check for WWW-Authenticate header
            String authHeader = request.getHeaders().get(HttpHeader.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Negotiate ")) {
                response.getHeaders().put(HttpHeader.WWW_AUTHENTICATE, "Negotiate");
                response.setStatus(401);
                //response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
                Content.Sink.write(response, true, "Kerberos Authentication Required", callback);
                return true;
            }

            String kerberosToken = authHeader.substring("Negotiate ".length());
            byte[] token = Base64.getDecoder().decode(kerberosToken);
            
            // Validate kvno before proceeding - check that we have compatible keytab entries
	    // we shouldn't have to do this!
	    //
	    // https://github.com/openjdk/jdk/blob/master/src/java.security.jgss/share/classes/sun/security/krb5/EncryptionKey.java
	    // comment around line 546: "When no matched kvno is found, returns tke key of the same etype with the highest kvno"
	    // that seems wrong.  there are GSSException types made to report the error, rather than blindly trying decryption
	    //
	    // RFC 4120 says that when a server receives an AP-REQ, “If the key version indicated by the Ticket in the KRB_AP_REQ is not one the server can use (e.g., it indicates an old key, and the server no longer possesses a copy of the old key), the KRB_AP_ERR_BADKEYVER error is returned.”
	    // IETF Datatracker

	    // Related: if the server simply doesn’t have the right key material to decrypt the ticket (e.g., wrong realm key), it should return KRB_AP_ERR_NOKEY.
	    // IETF Datatracker

	    // For reference, the error-code values are:

            //    KRB_AP_ERR_BADKEYVER = 44 (“Specified version of key is not available”)
	    //
	    //    Claude's analysis notes are in ../jdk/src/java.security.jgss/TEST_COVERAGE_claude.txt

            try {
                String keytabPath = SrvApp.srvProp.getProperty("keytab");
                var keytabEntries = KerberosKeytabReader.readKeytabFile(keytabPath);
                System.out.println("Available keytab key versions: " +
                    keytabEntries.stream().map(e -> e.getKeyVersion()).distinct().toList());

                // Validate kvno using keytab entries from srv_cred
                if (SrvApp.srv_cred != null && !SrvApp.srv_cred.isEmpty()) {
                    var availableKvnos = SrvApp.srv_cred.stream().map(e -> e.getKeyVersion()).distinct().toList();

                    // Extract kvno from SPNEGO token using byte pattern matching
                    try {
                        boolean kvnoFound = false;
                        int foundClientKvno = -1;

                        // Debug: print full token in hex
                        StringBuilder hexDump = new StringBuilder();
                        for (int i = 0; i < token.length; i++) {
                            hexDump.append(String.format("%02X ", token[i] & 0xFF));
                        }
                        System.out.println("Token hex dump (full): " + hexDump.toString());

                        // Look for kvno pattern: A1 03 02 01 [kvno]
                        for (int i = 0; i < token.length - 4; i++) {
                            if (token[i] == (byte)0xA1 && token[i+1] == (byte)0x03 &&
                                token[i+2] == (byte)0x02 && token[i+3] == (byte)0x01) {
                                foundClientKvno = token[i+4] & 0xFF;
                                System.out.println("Found client ticket kvno: " + foundClientKvno + " at offset " + i);
                                System.out.println("Available kvnos: " + availableKvnos + " (type: " + availableKvnos.getClass() + ")");
                                System.out.println("Client kvno: " + foundClientKvno + " (type: " + Integer.class + ")");
                                System.out.println("Contains check: " + availableKvnos.contains(foundClientKvno));

                                // Try explicit comparison since contains() is failing
                                boolean hasMatch = false;
                                for (int kvno : availableKvnos) {
                                    if (kvno == foundClientKvno) {
                                        hasMatch = true;
                                        break;
                                    }
                                }
                                System.out.println("Explicit kvno match: " + hasMatch);

                                if (hasMatch || availableKvnos.contains(foundClientKvno)) {
                                    System.out.println("Kvno validation passed - client kvno " + foundClientKvno + " matches keytab");
                                    kvnoFound = true;
                                    break;
                                }
                            }
                        }

                        if (!kvnoFound) {
                            if (foundClientKvno == -1) {
                                System.out.println("Kvno validation failed - no kvno found in token, keytab versions: " + availableKvnos);
                            } else {
                                System.out.println("Kvno mismatch - client kvno " + foundClientKvno + " not in keytab versions: " + availableKvnos);
                            }
                            response.setStatus(401);
                            Content.Sink.write(response, true, "Service key version mismatch", callback);
                            return true;
                        }

                    } catch (Exception e) {
                        System.out.println("Error extracting kvno from token: " + e.getMessage());
                        // Continue anyway - let GSS context establishment handle the validation
                    }
                }

            } catch (Exception e) {
                System.out.println("Unable to read keytab for validation: " + e.getMessage());
            }

            try {
                // even newer using kerb4j
                System.out.println("DO kerb4j ");
                var gssContext = SrvApp.sp_client.createAcceptContext();
                gssContext.acceptToken(token);
                System.out.println("is establishedDDDD: " + gssContext.isEstablished());
                System.out.println("is SrcName:" + gssContext.getSrcName());
                
                /*
                // new method using Apache DS libraries, avoids jaas.conf
                var mycred = SrvApp.srv_cred;
                System.out.println("srv_cred: " + mycred.getRemainingLifetime());
                GSSContext gssContext = KerberosKeytabReader.createGSSContext(mycred, "HTTP/mjolnir.ad.coplanar.net", "AD.COPLANAR.NET");
                */
                
                /*
                 // JAAS method, need jaas.conf set in system properties, etc.
                // excuse me, while i kiss this guy: https://www.baeldung.com/java-gss
                GSSManager manager = GSSManager.getInstance();
                // try system property javax.security.auth.useSubjectCredsOnly=false
                
               GSSContext gssContext = manager.createContext((GSSCredential) null);
                */
                
                /*
                // both old methods
                System.out.println("Remaining lifetime in seconds = " + gssContext.getLifetime());

                gssContext.acceptSecContext(token, 0, token.length);
                //System.out.println("subject: " );
                System.out.println("Context target name = " + gssContext.getTargName());
                */

                if (!gssContext.isEstablished()) {
                    response.setStatus(401);
                    //response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
                    Content.Sink.write(response, true, "Kerberos Authentication Failed", callback);
                    return true;
                }

                GSSName srcName = gssContext.getSrcName();
                String user = srcName.toString();
                //System.out.println("user: " + user);
                // TODO - mapping function?
                username = user.split("@")[0] + "@nrsi.on.ca";
                // see above cert auth for notes about creating Subject etc.
                //request.setAttribute("user", username);
                //gssContext.dispose();
                authSucceeded = true;


            } catch (GSSException e) {
                e.printStackTrace();
                response.setStatus(401);
                //response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
                Content.Sink.write(response, true, "Kerberos Authentication Error", callback);
                System.out.println(e);
		// GSSException: Failure unspecified at GSS-API level (Mechanism level: Checksum failed)
		//  -did our service keytab get out of date due to service account pw change?
                return true;
            }
        }
        
        if (authSucceeded && username != null) {
            //return super.handle(request, response, callback);
            // alternatively
            var ses = request.getSession(true);
            ses.setAttribute("username", username);
            // ThreadSessionLifecycleListener logic here, now with access to Subject earlier
            
            String sid = ses.getId();
            System.out.println("Session created: " + sid);
            // Add custom session creation logic here

            BlockingQueue<UpdateMessage> eventQueue;

            for (String name : ses.asAttributeMap().keySet()) {
                System.out.println("Session: " + name);
            }
            Set<String> attributeNames = ses.getAttributeNameSet();
            for (String attributeName : attributeNames) {
                System.out.println("Attribute Name: " + attributeName + ", Value: " + ses.getAttribute(attributeName));
            }
            
            // is this safe for Virtual Threads (avoids pinning)?
            @SuppressWarnings("unchecked")
            BlockingQueue<UpdateMessage> tempQueue = (BlockingQueue<UpdateMessage>) ses.getAttribute("eventQueue");
            eventQueue = tempQueue;
            if (eventQueue == null) {
                GenericThread sessThread = (GenericThread) appSessions.getThread(sid);
                if (sessThread == null) {
                    eventQueue = new LinkedBlockingQueue<>();
                    // class should be based on the URL path - /ctlr/tsc2 = TscThread
                    // generate this mapping using an Annotation?

                    // chicken vs egg: can't set on Session until after this
                    // String threadType = (String) ses.getAttribute("threadType");
                    sessThread = new GenericThread();
                    
                    sessThread.setMyQueueSes(eventQueue, ses, username);
                    Future<?> future = vexec.submit(sessThread);
                 /*   doesn't work
                    future.thenAccept(result -> {
                        System.out.println("Task result: " + result);
                        
                    });
*/
                    appSessions.putThread(sid, sessThread);
                } else {
                    System.out.println("new session error: found thread and no eventQueue!");
                }
          //      ses.setAttribute("eventQueue", eventQueue);

            } else {
                System.out.println("new session error: found existing eventQueue!");
            }

        }
        return false; // WTF
    }
}
