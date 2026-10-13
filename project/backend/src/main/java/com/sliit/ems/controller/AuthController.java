package com.sliit.ems.controller;

import com.sliit.ems.dao.hr.RecruitmentDAO;
import com.sliit.ems.dao.it.UserAccountDAO;
import com.sliit.ems.service.AuthService;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.PasswordUtil;
import com.sliit.ems.util.Validation;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class AuthController {

    private final AuthService authService = new AuthService();
    private final RecruitmentDAO recruitmentDAO = new RecruitmentDAO();
    private final UserAccountDAO userAccountDAO = new UserAccountDAO();

    private static final String OUTSIDE_USER_ROLE = "OUTSIDE_USER";

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
                user.put("applicantId", result.account.applicantId);
                user.put("username", result.account.username);
                user.put("role", result.account.roleName);
                response.put("user", user);
                HttpUtil.sendJson(exchange, 200, response);
            } catch (SQLException e) {
                handleDbError(exchange, e);
            }
        };
    }

    /**
     * POST /api/auth/register - public, unauthenticated self-registration for outside
     * (job-applicant) users. Creates an APPLICANT + USER_ACCOUNT row transactionally and
     * assigns the OUTSIDE_USER role. Staff/employee accounts are never created here - those
     * are still created by the IT Coordinator via POST /api/users (AdminController).
     */
    public HttpHandler register() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
                return;
            }
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                String name = Json.getString(body, "name");
                String email = Json.getString(body, "email");
                String phone = Json.getString(body, "phoneNumber");
                String password = Json.getString(body, "password");

                Validation.requireNonBlank(name, "Name");
                Validation.requireMaxLength(name, 100, "Name");
                Validation.requireEmail(email, "Email");
                Validation.requireMaxLength(email, 100, "Email");
                Validation.requirePhone(phone, "Phone number");
                Validation.requireStrongPassword(password);

                if (recruitmentDAO.applicantEmailExists(email)) {
                    HttpUtil.sendError(exchange, 409, "An account with this email already exists. Please log in instead.");
                    return;
                }
                if (userAccountDAO.usernameExists(email)) {
                    HttpUtil.sendError(exchange, 409, "An account with this email already exists. Please log in instead.");
                    return;
                }

                Integer roleId = userAccountDAO.findRoleIdByName(OUTSIDE_USER_ROLE);
                if (roleId == null) {
                    HttpUtil.sendError(exchange, 500, "Registration is temporarily unavailable. Please contact support.");
                    return;
                }

                String passwordHash = PasswordUtil.hash(password);
                RecruitmentDAO.RegisteredApplicant created =
                    recruitmentDAO.registerApplicantUser(name, email, phone, passwordHash, roleId);

                HttpUtil.sendJson(exchange, 201, Map.of(
                    "applicantId", created.applicantId,
                    "userId", created.userId,
                    "message", "Registration successful. You can now log in."));
            } catch (SQLException e) {
                handleDbError(exchange, e);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendError(exchange, 400, e.getMessage());
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

    public static void handleDbError(HttpExchange exchange, SQLException e) throws IOException {
        System.err.println("Database error: " + e.getMessage());
        HttpUtil.sendError(exchange, 500, "A server error occurred. Please try again.");
    }
}
