package com.sliit.ems.controller;

import com.sliit.ems.service.AuthService;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class AuthController {

    private final AuthService authService = new AuthService();

    public HttpHandler login() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
                return;
            }
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                String username = Json.getString(body, "username");
                String password = Json.getString(body, "password");

                AuthService.LoginResult result = authService.login(username, password);
                if (!result.success) {
                    HttpUtil.sendError(exchange, 401, result.message);
                    return;
                }

                Map<String, Object> response = Json.obj();
                response.put("token", result.token);
                Map<String, Object> user = Json.obj();
                user.put("userId", result.account.userId);
                user.put("employeeId", result.account.employeeId);
                user.put("username", result.account.username);
                user.put("role", result.account.roleName);
                response.put("user", user);
                HttpUtil.sendJson(exchange, 200, response);
            } catch (SQLException e) {
                handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler logout() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            String token = HttpUtil.getBearerToken(exchange);
            authService.logout(token);
            HttpUtil.sendJson(exchange, 200, Map.of("message", "Logged out"));
        };
    }

    static void handleDbError(HttpExchange exchange, SQLException e) throws IOException {
        System.err.println("Database error: " + e.getMessage());
        HttpUtil.sendError(exchange, 500, "A server error occurred. Please try again.");
    }
}
