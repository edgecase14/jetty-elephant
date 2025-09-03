/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.coplanar.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import static java.util.Collections.list;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.coplanar.updatemsg.UpdateMessage;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 *
 * @author jjackson
 */
public final class SseHandler extends Handler.Abstract.NonBlocking {

    private static Map<Response, Callback> callbacks = new HashMap<>();
    private static ThreadPool threadPool;
    
    SseHandler(ThreadPool tp) {
        threadPool = tp;
    }
    
    @Override
    public boolean handle(Request request, Response response, Callback callback) {
        String last_event_id = request.getHeaders().get("Last-Event-ID");
        int last_id = 0;
        if (last_event_id != null) {
            try {
                last_id = Integer.parseInt(last_event_id);
                //System.out.println("Converted number: " + number);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format: " + last_event_id);
            }
        }
        
        response.setStatus(200);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/event-stream; charset=UTF-8");
        CustomSession customSes = (CustomSession) request.getAttribute("customSession");
        String sid = "none";
        if (customSes != null) {
            // close previous EventListener
            Response sse = customSes.getSseResponse();
            if (sse != null) {
                var cb = callbacks.get(sse);
                Content.Sink.write(sse, true, "", cb);
            }

            sid = customSes.getId();
            customSes.setSseResponse(response);
            // need to store callback somewhere so we can close the request later.
            callbacks.put(response, callback);
            //System.out.println("SseHandler WITH session");
            // no callback is suspicious - what keeps multiple writes in sequence?  maybe use synchronous version?
            write(response, "data: { \"type\" : \"bar\" }\n\n", Callback.NOOP);  // doesn't complete open unless write something... empty string works on chrome, not on ff
            // no callback is suspicious
            //if (last_id == queued tail?) {
            //    response.write(false, null, null);
                
            //} else {
            //        // re-send queued before last_id
            //}
        } else {
            System.out.println("SseHandler no session");
            // usually happens when server is restarted and lost session:
            // -can we just send a browser reset() message?  avoids limbo state
            // -could also set the thread type based on URL, to hide some startup latency
            callback.failed(new Exception("SseHandler no session"));
            // should we still return true?  will this close connection with error code?
        }

        return true;
    }
    
    // FIXME on close, we need to remove our Response from Session

    private static final void write(Response response, String msg, Callback cb) {
        System.out.println(msg);
        threadPool.execute(() -> {
            Content.Sink.write(response, false, msg, cb);
        });
        
        //ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(msg);
        //Content.Sink.write(response, false, byteBuffer);
    }
    
    public static final void writeObj(Response response, UpdateMessage update, Callback cb) throws JsonProcessingException {
        threadPool.execute(() -> {
            String msg = null;
            try {
                msg = "data: " + JsonCodec.encode(update) + "\n\n";
            } catch (JsonProcessingException ex) {
                Logger.getLogger(SseHandler.class.getName()).log(Level.SEVERE, null, ex);
                // signal failure to the callback?
                cb.failed(ex);
            }
            write(response, msg, cb);
        });
    }

    public static final void writeObjList(Response response, List<UpdateMessage> ul, Callback cb) throws JsonProcessingException {
        threadPool.execute(() -> {
            String msg = "[ ";
            Iterator<UpdateMessage> it = ul.iterator();
            try {
                while (it.hasNext()) {
                    msg = msg + JsonCodec.encode(it.next());
                    if (it.hasNext()) {
                        msg = msg + ", ";
                    }
                }
            } catch (JsonProcessingException ex) {
                Logger.getLogger(SseHandler.class.getName()).log(Level.SEVERE, null, ex);
                // signal failure to the callback?
                cb.failed(ex);
            }
            msg = msg + " ]";
            msg = "data: " + msg + "\n\n";
            write(response, msg, cb);
        });
    }
}
