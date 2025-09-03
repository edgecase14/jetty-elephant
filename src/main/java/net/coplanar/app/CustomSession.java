package net.coplanar.app;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import net.coplanar.updatemsg.UpdateMessage;
import org.eclipse.jetty.server.Response;

public class CustomSession {
    private final String sessionId;
    private final BlockingQueue<UpdateMessage> queue;
    private GenericThread thread;
    private final long creationTime;
    private volatile long lastAccessTime;
    private String username;
    private String userId;
    private Response sseResponse;
    private Future<?> threadFuture;

    public CustomSession(String sessionId) {
        this.sessionId = sessionId;
        this.queue = new LinkedBlockingQueue<>();
        this.creationTime = System.currentTimeMillis();
        this.lastAccessTime = creationTime;
    }

    public String getId() {
        return sessionId;
    }

    public BlockingQueue<UpdateMessage> getQueue() {
        return queue;
    }

    public GenericThread getThread() {
        return thread;
    }

    public void setThread(GenericThread thread) {
        this.thread = thread;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Response getSseResponse() {
        return sseResponse;
    }

    public void setSseResponse(Response sseResponse) {
        this.sseResponse = sseResponse;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void updateLastAccessTime() {
        lastAccessTime = System.currentTimeMillis();
    }

    public boolean isExpired(long maxInactiveInterval) {
        return maxInactiveInterval > 0 && 
               (System.currentTimeMillis() - lastAccessTime) > (maxInactiveInterval * 1000);
    }

    public Future<?> getThreadFuture() {
        return threadFuture;
    }

    public void setThreadFuture(Future<?> threadFuture) {
        this.threadFuture = threadFuture;
    }
}