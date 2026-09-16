package com.sliit.ems.controller;

import com.sliit.ems.dao.AuditLogDAO;
import com.sliit.ems.dao.TrainingCertDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TrainingController {

    private final TrainingCertDAO dao = new TrainingCertDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String HR = "HR_MANAGER";

    // ---- Training ----

    public HttpHandler listTraining() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                if (HR.equals(session.roleName)) {
                    HttpUtil.sendJson(exchange, 200, dao.findAllTraining());
                } else {
                    HttpUtil.sendJson(exchange, 200, dao.findTrainingHistoryForEmployee(session.employeeId));
                }
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createTraining() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = dao.createTraining(
                    Json.getString(body, "trainingTitle"), Json.getString(body, "provider"),
                    Json.getString(body, "startDate"), Json.getString(body, "endDate"), Json.getString(body, "trainingType"));
                auditLogDAO.record(session.userId, "CREATE", "TRAINING", "Created training ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("trainingId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid training data: " + e.getMessage());
            }
        };
    }

    @SuppressWarnings("unchecked")
    public HttpHandler assignTraining() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            int trainingId = extractIdBeforeSuffix(exchange, "/assign");
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                List<Object> rawIds = (List<Object>) body.get("employeeIds");
                if (rawIds == null || rawIds.isEmpty()) {
                    HttpUtil.sendError(exchange, 400, "employeeIds is required and must not be empty.");
                    return;
                }
                List<Integer> employeeIds = rawIds.stream().map(o -> ((Number) o).intValue()).toList();
                dao.assignEmployees(trainingId, employeeIds);
                auditLogDAO.record(session.userId, "CREATE", "EMPLOYEE_TRAINING", "Assigned " + employeeIds.size() + " employee(s) to training ID " + trainingId);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Employees assigned to training"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid assignment data: " + e.getMessage());
            }
        };
    }

    public HttpHandler attendees() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            int trainingId = extractIdBeforeSuffix(exchange, "/attendees");
            try {
                HttpUtil.sendJson(exchange, 200, dao.findAttendeesForTraining(trainingId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    // ---- Certification ----

    public HttpHandler listCertifications() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                if (HR.equals(session.roleName)) {
                    HttpUtil.sendJson(exchange, 200, dao.findAllCertifications());
                } else {
                    HttpUtil.sendJson(exchange, 200, dao.findCertifications(session.employeeId));
                }
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createCertification() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = dao.createCertification(
                    Json.getInt(body, "employeeId"), Json.getString(body, "certificationName"),
                    Json.getString(body, "issuingOrganization"), Json.getString(body, "issueDate"), Json.getString(body, "expiryDate"));
                auditLogDAO.record(session.userId, "CREATE", "CERTIFICATION", "Created certification ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("certificationId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid certification data: " + e.getMessage());
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
