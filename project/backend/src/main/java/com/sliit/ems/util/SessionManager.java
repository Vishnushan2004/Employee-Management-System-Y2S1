package com.sliit.ems.util;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Very simple in-memory session/token store, appropriate for a local
 * single-instance college project (no Redis/JWT infrastructure needed).
 * Tokens are opaque random UUIDs kept in server memory and expire after
 * a fixed period of inactivity.
 */
public class SessionManager {

    public static class Session {
        public final int userId;
        public final int employeeId; // 0 if this session belongs to an outside user (no EMPLOYEE record)
        public final int applicantId; // 0 if this session belongs to a staff/employee user (no APPLICANT record)
        public final String username;
        public final String roleName;
        public volatile Instant lastAccess;

        Session(int userId, int employeeId, int applicantId, String username, String roleName) {
            this.userId = userId;
            this.employeeId = employeeId;
            this.applicantId = applicantId;
            this.username = username;
            this.roleName = roleName;
            this.lastAccess = Instant.now();
        }
    }

    // 30 minutes of inactivity -> session expires (simple session timeout)
    private static final long TIMEOUT_SECONDS = 30 * 60;

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public static String createSession(int userId, int employeeId, int applicantId, String username, String roleName) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(userId, employeeId, applicantId, username, roleName));
        return token;
    }

    /** Returns the session if the token is valid and not expired, refreshing its last-access time. */
    public static Session get(String token) {
        if (token == null) return null;
        Session s = sessions.get(token);
        if (s == null) return null;
        if (Instant.now().getEpochSecond() - s.lastAccess.getEpochSecond() > TIMEOUT_SECONDS) {
            sessions.remove(token);
            return null;
        }
        s.lastAccess = Instant.now();
        return s;
    }

    public static void invalidate(String token) {
        if (token != null) sessions.remove(token);
    }
}
