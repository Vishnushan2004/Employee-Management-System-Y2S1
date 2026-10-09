package com.sliit.ems.controller;

import com.sliit.ems.dao.AuditLogDAO;
import com.sliit.ems.dao.PerformanceDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.Map;

public class PerformanceController {

    private final PerformanceDAO performanceDAO = new PerformanceDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String HR = "HR_MANAGER";

    public HttpHandler listGoals() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                boolean isHr = HR.equals(session.roleName);
                Map<String, String> query = HttpUtil.parseQuery(exchange);
                Integer employeeId = isHr && query.containsKey("employeeId") ? Integer.parseInt(query.get("employeeId"))
                        : isHr ? null : session.employeeId;
                HttpUtil.sendJson(exchange, 200, performanceDAO.findGoals(employeeId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createGoal() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = performanceDAO.createGoal(
                    Json.getInt(body, "employeeId"), Json.getString(body, "goalTitle"),
                    Json.getString(body, "description"), Json.getString(body, "targetValue"));
                auditLogDAO.record(session.userId, "CREATE", "PERFORMANCE_GOAL", "Created goal ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("goalId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid goal data: " + e.getMessage());
            }
        };
    }

    public HttpHandler updateGoal() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            int id = HttpUtil.extractIdFromPath(exchange);
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                performanceDAO.updateGoalProgress(id, Json.getDouble(body, "progress"), Json.getString(body, "status"));
                auditLogDAO.record(session.userId, "UPDATE", "PERFORMANCE_GOAL", "Updated goal ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Goal updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler listReviews() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                boolean isHr = HR.equals(session.roleName);
                Map<String, String> query = HttpUtil.parseQuery(exchange);
                Integer employeeId = isHr && query.containsKey("employeeId") ? Integer.parseInt(query.get("employeeId"))
                        : isHr ? null : session.employeeId;
                HttpUtil.sendJson(exchange, 200, performanceDAO.findReviews(employeeId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createReview() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                Double score = body.get("performanceScore") != null ? Json.getDouble(body, "performanceScore") : null;
                int id = performanceDAO.createReview(
                    Json.getInt(body, "employeeId"), Json.getString(body, "reviewDate"),
                    Json.getString(body, "reviewPeriod"), score, Json.getString(body, "feedback"));
                auditLogDAO.record(session.userId, "CREATE", "PERFORMANCE_REVIEW", "Created review ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("reviewId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid review data: " + e.getMessage());
            }
        };
    }
}
