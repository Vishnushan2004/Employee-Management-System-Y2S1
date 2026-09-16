package com.sliit.ems.controller;

import com.sliit.ems.dao.AuditLogDAO;
import com.sliit.ems.dao.CareerDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.Map;

public class CareerController {

    private final CareerDAO careerDAO = new CareerDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String HR = "HR_MANAGER";

    public HttpHandler listPromotions() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                Integer employeeId = HR.equals(session.roleName) ? null : session.employeeId;
                HttpUtil.sendJson(exchange, 200, careerDAO.findPromotions(employeeId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createPromotion() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = careerDAO.recordPromotion(
                    Json.getInt(body, "employeeId"), Json.getString(body, "previousDesignation"),
                    Json.getInt(body, "newDesignationId"), Json.getString(body, "newDesignationName"),
                    Json.getString(body, "effectiveDate"), Json.getString(body, "reason"));
                auditLogDAO.record(session.userId, "CREATE", "PROMOTION", "Recorded promotion ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("promotionId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid promotion data: " + e.getMessage());
            }
        };
    }

    public HttpHandler listTransfers() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                Integer employeeId = HR.equals(session.roleName) ? null : session.employeeId;
                HttpUtil.sendJson(exchange, 200, careerDAO.findTransfers(employeeId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createTransfer() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = careerDAO.recordTransfer(
                    Json.getInt(body, "employeeId"), Json.getString(body, "previousDepartment"),
                    Json.getInt(body, "newDepartmentId"), Json.getString(body, "newDepartmentName"),
                    Json.getString(body, "effectiveDate"), Json.getString(body, "reason"));
                auditLogDAO.record(session.userId, "CREATE", "TRANSFER", "Recorded transfer ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("transferId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid transfer data: " + e.getMessage());
            }
        };
    }
}
