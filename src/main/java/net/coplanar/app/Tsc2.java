/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.util.CompletableTask;
import net.coplanar.updatemsg.UpdateMessage;

/**
 *
 * @author jjackson
 */
public class Tsc2 extends Handler.Abstract.NonBlocking {
    
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        response.setStatus(200);
        //response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=UTF-8");
        Session ses = request.getSession(false);
        String un = "none";
        BlockingQueue<Object> eventQueue = null;
        if (ses != null) {
            un = ses.getId();         
            eventQueue = (BlockingQueue<Object>) ses.getAttribute("eventQueue");
        } else {
            System.out.println("Tsc2 no session");
        }
        if (eventQueue == null) {
            System.out.println("no eventQueue");
            callback.failed(new Exception("no eventQueue"));
            // send browser reload() instead?
        } else {
    
            ChunksToString cts = new ChunksToString(request);

            // XX need failure to trigger HTTP status 500
            cts.start().thenAccept(toEventQueue(eventQueue)).thenRun(() -> {
                callback.succeeded();
            });
        }
        return true;
    }
    
    public Consumer<String> toEventQueue(BlockingQueue<Object> eventQueue) {
        
        return (String result) -> {
            try {
                // convert JSON to object - doesn't handle arrays
                UpdateMessage robj = JsonCodec.decode(result);
                eventQueue.offer(robj);
                System.out.println("The request is: " + result);
            } catch (JsonProcessingException ex) {
                Logger.getLogger(Tsc2.class.getName()).log(Level.SEVERE, null, ex);
                // XX set that this completeable future failed
            }
        };
    }
}
