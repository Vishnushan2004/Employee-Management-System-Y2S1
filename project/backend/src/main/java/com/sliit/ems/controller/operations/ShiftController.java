package com.sliit.ems.controller.operations;

import com.sliit.ems.controller.AuthController;

import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.dao.operations.ShiftDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.Map;

public class ShiftController {

    private final ShiftDAO shiftDAO = new ShiftDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String MANAGE_ROLE = "OPERATIONS_MANAGER";

    public HttpHandler listShifts() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, shiftDAO.findAllShifts());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createShift() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, MANAGE_ROLE);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = shiftDAO.createShift(
                    Json.getString(body, "shiftName"), Json.getString(body, "shiftType"),
                    Json.getString(body, "startTime"), Json.getString(body, "endTime"),
                    Json.getDouble(body, "workingHours"));
                auditLogDAO.record(session.userId, "CREATE", "SHIFT", "Created shift ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("shiftId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid shift data: " + e.getMessage());
            }
        };
    }

    public HttpHandler updateShift() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, MANAGE_ROLE);
            if (session == null) return;
            int id = HttpUtil.extractIdFromPath(exchange);
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                shiftDAO.updateShift(id,
                    Json.getString(body, "shiftName"), Json.getString(body, "shiftType"),
                    Json.getString(body, "startTime"), Json.getString(body, "endTime"),
                    Json.getDouble(body, "workingHours"));
                auditLogDAO.record(session.userId, "UPDATE", "SHIFT", "Updated shift ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Shift updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid shift data: " + e.getMessage());
            }
        };
    }

    public HttpHandler listAssignments() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                boolean isOps = "OPERATIONS_MANAGER".equals(session.roleName) || "HR_MANAGER".equals(session.roleName)
                        || "ADMINISTRATIVE_OFFICER".equals(session.roleName) || "FINANCE_PAYROLL_MANAGER".equals(session.roleName);
                var result = isOps ? shiftDAO.findAllAssignments() : shiftDAO.findAssignmentsByEmployee(session.employeeId);
                HttpUtil.sendJson(exchange, 200, result);
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler assignShift() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, MANAGE_ROLE);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = shiftDAO.assignShift(
                    Json.getInt(body, "employeeId"), Json.getInt(body, "shiftId"),
                    Json.getString(body, "assignmentStartDate"), Json.getString(body, "assignmentEndDate"));
                auditLogDAO.record(session.userId, "CREATE", "SHIFT_ASSIGNMENT", "Created shift assignment ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("assignmentId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid assignment data: " + e.getMessage());
            }
        };
    }
}
