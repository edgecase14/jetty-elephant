package net.coplanar.app;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class CustomSessionHandler extends Handler.Wrapper {
    
    private final ConcurrentHashMap<String, CustomSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final String cookieName = "JSESSIONID";
    private final int maxInactiveInterval = 1800; // 30 minutes in seconds
    
    public CustomSessionHandler(Handler handler) {
        super(handler);
        // Start cleanup task every 5 minutes
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        // Get or create session
        CustomSession session = getSession(request, response);
        if (session != null) {
            session.updateLastAccessTime();
            request.setAttribute("customSession", session);
        }
        
        return super.handle(request, response, callback);
    }

    private CustomSession getSession(Request request, Response response) {
        // Look for existing session ID in cookies
        String sessionId = null;
        var cookies = Request.getCookies(request);
        if (cookies != null) {
            for (HttpCookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    sessionId = cookie.getValue();
                    break;
                }
            }
        }

        CustomSession session = null;
        if (sessionId != null) {
            session = sessions.get(sessionId);
            if (session != null && session.isExpired(maxInactiveInterval)) {
                // Session expired, remove it
                sessions.remove(sessionId);
                session = null;
            }
        }

        // Create new session if none found or expired
        if (session == null) {
            sessionId = generateSessionId();
            session = new CustomSession(sessionId);
            sessions.put(sessionId, session);
            
            // Set session cookie
            HttpCookie sessionCookie = HttpCookie.build(cookieName, sessionId)
                    .path("/")
                    .httpOnly(true)
                    .secure(true)
                    .sameSite(HttpCookie.SameSite.STRICT)
                    .build();
            
            Response.addCookie(response, sessionCookie);
        }

        return session;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public CustomSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void removeSession(String sessionId) {
        CustomSession session = sessions.remove(sessionId);
        if (session != null && session.getThread() != null) {
            session.getThread().interrupt();
        }
    }

    private void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry -> {
            CustomSession session = entry.getValue();
            if (session.isExpired(maxInactiveInterval)) {
                if (session.getThread() != null) {
                    session.getThread().interrupt();
                }
                return true;
            }
            return false;
        });
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        sessions.clear();
    }
}