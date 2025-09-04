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
import net.coplanar.controller.GenericController;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

/**
 *
 * @author jjackson
 */
public class ThreadTypeHandler extends Handler.Wrapper {


    // TODO - no-arg constructor required? Jetty bug maybe, there's a no-arg constructor one of the superclasses that doesn't make sense
    
    public ThreadTypeHandler(boolean dynamic, Handler handler) {
        super( dynamic, handler);
        //this.sessionThreads = new HashMap<>();
    }
    
    public ThreadTypeHandler(Handler handler) {
        this(false, handler);
    }

    // comes after Session created, and Listener has started GenericThread - we pass it a GenericController to use thru it's eventQueue
    // hopefully HTTP Cache control header : must-revalidate will cause the controller-name.html to always be hit on browser JS VM (re)start
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
    
        // if session exists, pass to next handler, if not, create one to fire a user lookup async to hide latency
        CustomSession customSes = (CustomSession) request.getAttribute("customSession");
        // TODO - filter to only handle URL path /ctlr/
        if (customSes != null) {
            String sid = customSes.getId();
            //String user = (String) request.getAttribute("user");
            //System.out.println("\nrequest session id: " + sid );  //+ " user: " + user);
            var st = customSes.getThread();
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

            Class<?> ctl_class = ControllerRegistry.getController(path.replace(".html","").replace("/",""));
            if (ctl_class != null) { //|| "/timesheet2/".equals(path)) { // or /timesheet2/timesheet
                
                //var eq = (BlockingQueue<UpdateMessage>) ses.getAttribute("initialQueue");
                GenericThread gt = customSes.getThread();
                BlockingQueue<UpdateMessage> eq = gt.getQueue();
                GenericController ctlr = gt.getController();
                if (ctlr == null) {
                    var setCtlr = new SetController();
                    var controller = (GenericController) ctl_class.getDeclaredConstructor().newInstance();
                    controller.querySetup(queryMap);  // can this be merged with queryNavigated()?
                    //if (user != null) {
                    //    setCtlr.user = user; // let controller do Hibernate find(TsUser.class) since we can't block in handler
                    //}
                    setCtlr.controller = controller;
                    System.out.println("setting controller to Timesheet" + eq);
                    var ret = eq.offer(setCtlr);
                    System.out.println("setting controller to Timesheet done." + ret);
                } else {
                    System.out.println("found thread, but has controller!");
                    // if same controller already, the rest is ok?
                    if (ctlr.getClass().equals(ctl_class)) {
                        System.out.println("not init, found eventQueue");
                        UpdateMessage um = ctlr.queryNavigated(queryMap);  // trigger browser reload()
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
                System.out.println("not a known controller" + path);
            }
        } else {
            System.out.println("request without session!");
        }
        return super.handle(request, response, callback);
    }
    
}
