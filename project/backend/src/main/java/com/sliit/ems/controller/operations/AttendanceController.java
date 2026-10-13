package com.sliit.ems.controller.operations;

import com.sliit.ems.controller.AuthController;

import com.sliit.ems.dao.AttendanceDAO;
import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.model.Attendance;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AttendanceController {

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String[] STAFF_ROLES = {
        "OPERATIONS_MANAGER", "ADMINISTRATIVE_OFFICER", "HR_MANAGER", "FINANCE_PAYROLL_MANAGER"
    };
    private static final String[] CORRECTION_ROLES = { "OPERATIONS_MANAGER", "ADMINISTRATIVE_OFFICER" };

    /** GET /api/attendance?date=&employeeId= - staff roles see everyone (optionally filtered); employees see only their own. */
    public HttpHandler list() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                Map<String, String> query = HttpUtil.parseQuery(exchange);
                boolean isStaff = List.of(STAFF_ROLES).contains(session.roleName);
                List<Attendance> records;
                if (!isStaff) {
                    records = attendanceDAO.findByEmployee(session.employeeId);
                } else if (query.containsKey("date")) {
                    records = attendanceDAO.findByDate(query.get("date"));
                } else if (query.containsKey("employeeId")) {
                    records = attendanceDAO.findByEmployee(Integer.parseInt(query.get("employeeId")));
                } else {
                    records = attendanceDAO.findAll();
                }
                HttpUtil.sendJson(exchange, 200, records.stream().map(this::toMap).toList());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** POST /api/attendance/check-in - the logged-in employee checks themselves in. */
    public HttpHandler checkIn() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                Attendance existing = attendanceDAO.findOpenToday(session.employeeId);
                if (existing != null && existing.checkOutTime == null) {
                    HttpUtil.sendError(exchange, 409, "You are already checked in today.");
                    return;
                }
                int id = attendanceDAO.checkIn(session.employeeId);
                HttpUtil.sendJson(exchange, 201, Map.of("attendanceId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** POST /api/attendance/check-out - the logged-in employee checks themselves out of today's open record. */
    public HttpHandler checkOut() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireAuth(exchange);
            if (session == null) return;
            try {
                Attendance existing = attendanceDAO.findOpenToday(session.employeeId);
                if (existing == null) {
                    HttpUtil.sendError(exchange, 409, "You have not checked in today.");
                    return;
                }
                if (existing.checkOutTime != null) {
                    HttpUtil.sendError(exchange, 409, "You have already checked out today.");
                    return;
                }
                attendanceDAO.checkOut(existing.attendanceId);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Checked out"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** PUT /api/attendance/{id} - Operations Manager / Administrative Officer correcting a record. */
    public HttpHandler correct() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, CORRECTION_ROLES);
            if (session == null) return;
            int id = HttpUtil.extractIdFromPath(exchange);
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                String status = Json.getString(body, "attendanceStatus");
                Integer lateMinutes = Json.getInt(body, "lateMinutes");
                attendanceDAO.correct(id, status, lateMinutes);
                auditLogDAO.record(session.userId, "CORRECT", "ATTENDANCE", "Corrected attendance ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Attendance corrected"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    private Map<String, Object> toMap(Attendance a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("attendanceId", a.attendanceId);
        m.put("employeeId", a.employeeId);
        m.put("employeeName", a.employeeName);
        m.put("attendanceDate", a.attendanceDate);
        m.put("checkInTime", a.checkInTime);
        m.put("checkOutTime", a.checkOutTime);
        m.put("lateMinutes", a.lateMinutes);
        m.put("overtimeHours", a.overtimeHours);
        m.put("workingHours", a.workingHours);
        m.put("attendanceStatus", a.attendanceStatus);
        return m;
    }
}
