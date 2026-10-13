package com.sliit.ems.controller.it;

import com.sliit.ems.controller.AuthController;

import com.sliit.ems.dao.it.AdminDAO;
import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.PasswordUtil;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.Map;

public class AdminController {

    private final AdminDAO adminDAO = new AdminDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String IT = "IT_COORDINATOR";

    // ---- Users ----

    public HttpHandler listUsers() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (Security.requireRole(exchange, IT) == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, adminDAO.findAllUsers());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createUser() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, IT);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                String password = Json.getString(body, "password");
                com.sliit.ems.util.Validation.requireStrongPassword(password);
                int id = adminDAO.createUserAccount(
                    Json.getInt(body, "employeeId"), Json.getString(body, "username"),
                    PasswordUtil.hash(password), Json.getInt(body, "roleId"));
                auditLogDAO.record(session.userId, "CREATE", "USER_ACCOUNT", "Created user account ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("userId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid account data: " + e.getMessage());
            }
        };
    }

    public HttpHandler setAccountStatus() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, IT);
            if (session == null) return;
            int id = extractIdBeforeSuffix(exchange, "/status");
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                adminDAO.setAccountStatus(id, Json.getString(body, "status"));
                auditLogDAO.record(session.userId, "UPDATE", "USER_ACCOUNT", "Changed status for user ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Account status updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler changeRole() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, IT);
            if (session == null) return;
            int id = extractIdBeforeSuffix(exchange, "/role");
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                adminDAO.changeRole(id, Json.getInt(body, "roleId"));
                auditLogDAO.record(session.userId, "UPDATE", "USER_ACCOUNT", "Changed role for user ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Role updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    // ---- Roles ----

    public HttpHandler createRole() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, IT);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = adminDAO.createRole(Json.getString(body, "roleName"), Json.getString(body, "description"));
                auditLogDAO.record(session.userId, "CREATE", "ROLE", "Created role ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("roleId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid role data: " + e.getMessage());
            }
        };
    }

    // ---- Permissions ----

    public HttpHandler listPermissions() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (Security.requireRole(exchange, IT) == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, adminDAO.findAllPermissions());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createPermission() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, IT);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = adminDAO.createPermission(Json.getString(body, "permissionName"), Json.getString(body, "module"), Json.getString(body, "action"));
                auditLogDAO.record(session.userId, "CREATE", "PERMISSION", "Created permission ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("permissionId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid permission data: " + e.getMessage());
            }
        };
    }

    public HttpHandler permissionsForRole() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (Security.requireRole(exchange, IT) == null) return;
            int roleId = HttpUtil.extractIdFromPath(exchange);
            try {
                HttpUtil.sendJson(exchange, 200, adminDAO.findPermissionsForRole(roleId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler assignPermission() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, IT);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                adminDAO.assignPermission(Json.getInt(body, "roleId"), Json.getInt(body, "permissionId"));
                auditLogDAO.record(session.userId, "UPDATE", "ROLE_PERMISSION", "Assigned permission to role");
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Permission assigned"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    private int extractIdBeforeSuffix(com.sun.net.httpserver.HttpExchange exchange, String suffix) {
        String path = exchange.getRequestURI().getPath();
        String trimmed = path.endsWith(suffix) ? path.substring(0, path.length() - suffix.length()) : path;
        String[] parts = trimmed.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }
}
