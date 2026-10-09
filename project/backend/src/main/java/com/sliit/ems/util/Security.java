package com.sliit.ems.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Set;

/**
 * Small helper so every controller enforces auth/role checks the same way.
 * IMPORTANT: this is the actual security boundary - the React frontend only
 * hides buttons for UX, it does not grant or deny access on its own.
 */
public class Security {

    /** Returns the active session for this request, or sends 401 and returns null if missing/expired. */
    public static SessionManager.Session requireAuth(HttpExchange exchange) throws IOException {
        String token = HttpUtil.getBearerToken(exchange);
        SessionManager.Session session = SessionManager.get(token);
        if (session == null) {
            HttpUtil.sendError(exchange, 401, "Not authenticated. Please log in again.");
            return null;
        }
        return session;
    }

    /** Returns the session only if its role is one of the allowed roles, else sends 403 and returns null. */
    public static SessionManager.Session requireRole(HttpExchange exchange, String... allowedRoles) throws IOException {
        SessionManager.Session session = requireAuth(exchange);
        if (session == null) return null; // 401 already sent
        Set<String> allowed = Set.of(allowedRoles);
        if (!allowed.contains(session.roleName)) {
            HttpUtil.sendError(exchange, 403, "You do not have permission to perform this action.");
            return null;
        }
        return session;
    }
}
