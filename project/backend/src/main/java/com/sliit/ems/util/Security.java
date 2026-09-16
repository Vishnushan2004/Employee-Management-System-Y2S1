package com.sliit.ems.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Set;

public class Security {

    // ============================================================
    // REQUIRE AUTHENTICATION
    // ============================================================

    public static SessionManager.Session requireAuth(
            HttpExchange exchange) throws IOException {

        String token = HttpUtil.getBearerToken(exchange);

        // --------------------------------------------------------
        // Missing token
        // --------------------------------------------------------

        if (token == null || token.isBlank()) {

            System.out.println(
                    "[AUTH] Request rejected: no Bearer token");

            HttpUtil.sendError(
                    exchange,
                    401,
                    "Not authenticated. Please log in again.");

            return null;
        }

        // --------------------------------------------------------
        // Find session
        // --------------------------------------------------------

        SessionManager.Session session = SessionManager.get(token);

        // --------------------------------------------------------
        // Invalid / expired token
        // --------------------------------------------------------

        if (session == null) {

            System.out.println(
                    "[AUTH] Request rejected: invalid or expired session");

            HttpUtil.sendError(
                    exchange,
                    401,
                    "Not authenticated. Please log in again.");

            return null;
        }

        // --------------------------------------------------------
        // Authentication successful
        // --------------------------------------------------------

        return session;
    }

    // ============================================================
    // REQUIRE ROLE
    // ============================================================

    public static SessionManager.Session requireRole(
            HttpExchange exchange,
            String... allowedRoles) throws IOException {

        SessionManager.Session session = requireAuth(exchange);

        // IMPORTANT:
        // Authentication can fail.
        // Do not access session.roleName if session is null.
        if (session == null) {
            return null;
        }

        Set<String> allowed = Set.of(allowedRoles);

        // --------------------------------------------------------
        // Check role
        // --------------------------------------------------------

        if (!allowed.contains(session.roleName)) {

            System.out.println(
                    "[AUTH] Access denied"
                            + " | User: "
                            + session.username
                            + " | Role: "
                            + session.roleName);

            HttpUtil.sendError(
                    exchange,
                    403,
                    "You do not have permission to perform this action.");

            return null;
        }

        // --------------------------------------------------------
        // Authorized
        // --------------------------------------------------------

        return session;
    }
}