package com.sliit.ems.util;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    // ============================================================
    // CONFIGURATION
    // ============================================================

    /*
     * Session timeout = 30 minutes.
     */
    private static final long TIMEOUT_SECONDS = 30 * 60;

    /*
     * In-memory session storage.
     *
     * NOTE:
     * Restarting the Java backend clears all sessions.
     */
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    // ============================================================
    // SESSION CLASS
    // ============================================================

    public static class Session {

        public final int userId;
        public final int employeeId;
        public final String username;
        public final String roleName;

        private volatile Instant lastAccess;

        public Session(
                int userId,
                int employeeId,
                String username,
                String roleName) {
            this.userId = userId;
            this.employeeId = employeeId;
            this.username = username;
            this.roleName = roleName;
            this.lastAccess = Instant.now();
        }

        public Instant getLastAccess() {
            return lastAccess;
        }

        private void updateLastAccess() {
            this.lastAccess = Instant.now();
        }
    }

    // ============================================================
    // CREATE SESSION
    // ============================================================

    public static String createSession(
            int userId,
            int employeeId,
            String username,
            String roleName) {

        String token = UUID.randomUUID().toString();

        Session session = new Session(
                userId,
                employeeId,
                username,
                roleName);

        sessions.put(token, session);

        System.out.println(
                "[SESSION] Created session for user: "
                        + username);

        return token;
    }

    // ============================================================
    // GET SESSION
    // ============================================================

    public static Session get(String token) {

        // --------------------------------------------------------
        // No token
        // --------------------------------------------------------

        if (token == null || token.isBlank()) {
            return null;
        }

        // --------------------------------------------------------
        // Find session
        // --------------------------------------------------------

        Session session = sessions.get(token);

        if (session == null) {
            return null;
        }

        // --------------------------------------------------------
        // Check timeout
        // --------------------------------------------------------

        long inactiveSeconds = Instant.now().getEpochSecond()
                - session.getLastAccess()
                        .getEpochSecond();

        if (inactiveSeconds > TIMEOUT_SECONDS) {

            sessions.remove(token);

            System.out.println(
                    "[SESSION] Session expired for user: "
                            + session.username);

            return null;
        }

        // --------------------------------------------------------
        // Refresh session
        // --------------------------------------------------------

        session.updateLastAccess();

        return session;
    }

    // ============================================================
    // INVALIDATE SESSION
    // ============================================================

    public static void invalidate(String token) {

        if (token == null || token.isBlank()) {
            return;
        }

        Session session = sessions.remove(token);

        if (session != null) {

            System.out.println(
                    "[SESSION] Session invalidated for user: "
                            + session.username);
        }
    }

    // ============================================================
    // CLEAR ALL
    // ============================================================

    public static void clearAll() {

        sessions.clear();

        System.out.println(
                "[SESSION] All sessions cleared.");
    }

    // ============================================================
    // SESSION COUNT
    // ============================================================

    public static int getSessionCount() {
        return sessions.size();
    }
}