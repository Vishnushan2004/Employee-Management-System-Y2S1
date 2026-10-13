package com.sliit.ems.controller.operations;

import com.sliit.ems.controller.AuthController;

import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.dao.LeaveRequestDAO;
import com.sliit.ems.dao.NotificationDAO;
import com.sliit.ems.model.LeaveRequest;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LeaveController {

    private final LeaveRequestDAO leaveDAO = new LeaveRequestDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    private static final String[] APPROVER_ROLES = { "OPERATIONS_MANAGER", "ADMINISTRATIVE_OFFICER" };
    private static final java.util.Set<String> VALID_TYPES = java.util.Set.of("ANNUAL", "CASUAL", "SICK", "OTHER");

    /** GET /api/leaves - approvers see everyone's; employees see only their own. Use ?scope=pending for the approval queue. */
    public HttpHandler list() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                Map<String, String> query = HttpUtil.parseQuery(exchange);
                boolean isApprover = List.of(APPROVER_ROLES).contains(session.roleName);
                List<LeaveRequest> leaves;
                if ("pending".equals(query.get("scope")) && isApprover) {
                    leaves = leaveDAO.findPending();
                } else if (isApprover) {
                    leaves = leaveDAO.findAll();
                } else {
                    leaves = leaveDAO.findByEmployee(session.employeeId);
                }
                HttpUtil.sendJson(exchange, 200, leaves.stream().map(this::toMap).toList());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** POST /api/leaves - any logged-in employee applies for themselves. */
    public HttpHandler apply() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                String leaveType = Json.getString(body, "leaveType");
                String startDate = Json.getString(body, "startDate");
                String endDate = Json.getString(body, "endDate");
                String reason = Json.getString(body, "reason");

                if (!VALID_TYPES.contains(leaveType)) {
                    HttpUtil.sendError(exchange, 400, "Invalid leave type.");
                    return;
                }
                if (reason != null && reason.length() > 500) {
                    HttpUtil.sendError(exchange, 400, "Reason must be 500 characters or fewer.");
                    return;
                }
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                if (end.isBefore(start)) {
                    HttpUtil.sendError(exchange, 400, "End date cannot be before start date.");
                    return;
                }

                int leaveId = leaveDAO.apply(session.employeeId, leaveType, startDate, endDate, reason);
                auditLogDAO.record(session.userId, "CREATE", "LEAVE_REQUEST", "Applied for " + leaveType + " leave, ID " + leaveId);
                HttpUtil.sendJson(exchange, 201, Map.of("leaveId", leaveId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (java.time.format.DateTimeParseException e) {
                HttpUtil.sendError(exchange, 400, "Invalid date format, expected YYYY-MM-DD.");
            }
        };
    }

    /** PUT /api/leaves/{id}/approve - Operations Manager / Administrative Officer only. */
    public HttpHandler approve() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, APPROVER_ROLES);
            if (session == null) return;
            int id = extractLeaveId(exchange);
            try {
                leaveDAO.approve(id, session.employeeId);
                auditLogDAO.record(session.userId, "APPROVE", "LEAVE_REQUEST", "Approved leave request ID " + id);
                notifyStatusChange(id, "approved");
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Leave approved"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** PUT /api/leaves/{id}/reject - Operations Manager / Administrative Officer only. */
    public HttpHandler reject() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, APPROVER_ROLES);
            if (session == null) return;
            int id = extractLeaveId(exchange);
            try {
                leaveDAO.reject(id, session.employeeId);
                auditLogDAO.record(session.userId, "REJECT", "LEAVE_REQUEST", "Rejected leave request ID " + id);
                notifyStatusChange(id, "rejected");
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Leave rejected"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** PUT /api/leaves/{id}/cancel - the employee who owns the request, or an approver. */
    public HttpHandler cancel() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            int id = extractLeaveId(exchange);
            try {
                leaveDAO.cancel(id);
                auditLogDAO.record(session.userId, "CANCEL", "LEAVE_REQUEST", "Cancelled leave request ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Leave cancelled"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    private final com.sliit.ems.dao.it.UserAccountDAO userAccountDAO = new com.sliit.ems.dao.it.UserAccountDAO();

    private void notifyStatusChange(int leaveId, String status) {
        try {
            LeaveRequest leave = leaveDAO.findById(leaveId);
            if (leave == null) return;
            Integer userId = userAccountDAO.findUserIdByEmployeeId(leave.employeeId);
            if (userId == null) return; // employee has no login account - nothing to notify
            notificationDAO.create(userId, "LEAVE_" + status.toUpperCase(), "Your leave request has been " + status + ".");
        } catch (SQLException e) {
            System.err.println("Failed to send leave status notification: " + e.getMessage());
        }
    }

    private int extractLeaveId(com.sun.net.httpserver.HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath(); // /api/leaves/{id}/approve
        String[] parts = path.split("/");
        return Integer.parseInt(parts[3]);
    }

    private Map<String, Object> toMap(LeaveRequest l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("leaveId", l.leaveId);
        m.put("employeeId", l.employeeId);
        m.put("employeeName", l.employeeName);
        m.put("leaveType", l.leaveType);
        m.put("startDate", l.startDate);
        m.put("endDate", l.endDate);
        m.put("reason", l.reason);
        m.put("status", l.status);
        m.put("appliedDate", l.appliedDate);
        m.put("approvedBy", l.approvedBy);
        m.put("approvedDate", l.approvedDate);
        return m;
    }
}
