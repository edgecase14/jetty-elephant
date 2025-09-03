package net.coplanar.app;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        // Write session list to file every 10 seconds
        cleanupExecutor.scheduleAtFixedRate(this::writeSessionList, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        // Get existing session only (don't create)
        CustomSession session = getSession(request, false);
        if (session != null) {
            session.updateLastAccessTime();
            request.setAttribute("customSession", session);
        }
        
        return super.handle(request, response, callback);
    }

    public CustomSession getSession(Request request, boolean create) {
        return getSession(request, null, create);
    }

    public CustomSession createSession(Request request, Response response) {
        return getSession(request, response, true);
    }

    private CustomSession getSession(Request request, Response response, boolean create) {
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

        // Create new session if none found or expired and create=true
        if (session == null && create) {
            sessionId = generateSessionId();
            session = new CustomSession(sessionId);
            sessions.put(sessionId, session);
            
            // Set session cookie if response provided
            if (response != null) {
                HttpCookie sessionCookie = HttpCookie.build(cookieName, sessionId)
                        .path("/")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite(HttpCookie.SameSite.STRICT)
                        .build();
                
                Response.addCookie(response, sessionCookie);
            }
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
        sessions.remove(sessionId);
    }

    private void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry -> {
            CustomSession session = entry.getValue();
            return session.isExpired(maxInactiveInterval);
        });
    }

    public int getSessionCount() {
        return sessions.size();
    }

    private void writeSessionList() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("sessions.txt"))) {
            writer.println("Session List - " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            writer.println("Total sessions: " + sessions.size());
            writer.println();
            
            for (var entry : sessions.entrySet()) {
                CustomSession session = entry.getValue();
                writer.printf("Session ID: %s%n", session.getId());
                writer.printf("  Username: %s%n", session.getUsername());
                writer.printf("  User ID: %s%n", session.getUserId());
                writer.printf("  Thread: %s%n", session.getThread() != null ? "active" : "none");
                writer.printf("  Creation: %s%n", 
                    LocalDateTime.ofEpochSecond(session.getCreationTime() / 1000, 0, 
                    java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now()))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                writer.printf("  Last Access: %s%n", 
                    LocalDateTime.ofEpochSecond(session.getLastAccessTime() / 1000, 0, 
                    java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now()))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                writer.println();
            }
        } catch (IOException e) {
            System.err.println("Error writing session list: " + e.getMessage());
        }
    }

    public void shutdown() {
        cleanupExecutor.shutdown();
        sessions.clear();
    }
}