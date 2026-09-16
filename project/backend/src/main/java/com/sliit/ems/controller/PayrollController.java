package com.sliit.ems.controller;

import com.sliit.ems.dao.AuditLogDAO;
import com.sliit.ems.dao.NotificationDAO;
import com.sliit.ems.dao.PayrollDAO;
import com.sliit.ems.dao.UserAccountDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class PayrollController {

    private final PayrollDAO payrollDAO = new PayrollDAO();

    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private final UserAccountDAO userAccountDAO = new UserAccountDAO();

    private final NotificationDAO notificationDAO = new NotificationDAO();

    private static final String MANAGE_ROLE = "FINANCE_PAYROLL_MANAGER";

    private static final String[] READ_ROLES = {
            "FINANCE_PAYROLL_MANAGER",
            "HR_MANAGER"
    };

    // ============================================================
    // LIST PAYROLL
    // ============================================================

    public HttpHandler list() {

        return exchange -> {

            if (HttpUtil.handlePreflight(exchange)) {
                return;
            }

            SessionManager.Session session = Security.requireAuth(exchange);

            if (session == null) {
                return;
            }

            try {

                boolean isStaff = List.of(READ_ROLES)
                        .contains(session.roleName);

                Map<String, String> query = HttpUtil.parseQuery(exchange);

                List<Map<String, Object>> rows;

                if (isStaff) {

                    Integer employeeId = query.containsKey("employeeId")
                            ? Integer.parseInt(
                                    query.get("employeeId"))
                            : null;

                    Integer month = query.containsKey("month")
                            ? Integer.parseInt(
                                    query.get("month"))
                            : null;

                    Integer year = query.containsKey("year")
                            ? Integer.parseInt(
                                    query.get("year"))
                            : null;

                    rows = payrollDAO.findAll(
                            employeeId,
                            month,
                            year,
                            query.get("status"));

                } else {

                    rows = payrollDAO.findByEmployee(
                            session.employeeId);
                }

                HttpUtil.sendJson(
                        exchange,
                        200,
                        rows);

            } catch (SQLException e) {

                AuthController.handleDbError(
                        exchange,
                        e);

            } catch (Exception e) {

                HttpUtil.sendError(
                        exchange,
                        400,
                        "Invalid payroll filter: "
                                + e.getMessage());
            }
        };
    }

    // ============================================================
    // PAYROLL DETAIL
    // ============================================================

    public HttpHandler detail() {

        return exchange -> {

            if (HttpUtil.handlePreflight(exchange)) {
                return;
            }

            SessionManager.Session session = Security.requireAuth(exchange);

            if (session == null) {
                return;
            }

            try {

                int id = HttpUtil.extractIdFromPath(
                        exchange);

                Map<String, Object> result = new LinkedHashMap<>();

                result.put(
                        "allowances",
                        payrollDAO.findAllowances(id));

                result.put(
                        "deductions",
                        payrollDAO.findDeductions(id));

                result.put(
                        "payslip",
                        payrollDAO.findPayslip(id));

                HttpUtil.sendJson(
                        exchange,
                        200,
                        result);

            } catch (SQLException e) {

                AuthController.handleDbError(
                        exchange,
                        e);

            } catch (Exception e) {

                HttpUtil.sendError(
                        exchange,
                        400,
                        "Invalid payroll ID: "
                                + e.getMessage());
            }
        };
    }

    // ============================================================
    // PROCESS PAYROLL
    // ============================================================

    public HttpHandler process() {

        return exchange -> {

            if (HttpUtil.handlePreflight(exchange)) {
                return;
            }

            SessionManager.Session session = Security.requireRole(
                    exchange,
                    MANAGE_ROLE);

            if (session == null) {
                return;
            }

            try {

                // ------------------------------------------------
                // Read JSON request
                // ------------------------------------------------

                Map<String, Object> body = HttpUtil.readJsonBody(exchange);

                // ------------------------------------------------
                // Required fields
                // ------------------------------------------------

                Integer employeeId = Json.getInt(
                        body,
                        "employeeId");

                Integer month = Json.getInt(
                        body,
                        "payrollMonth");

                Integer year = Json.getInt(
                        body,
                        "payrollYear");

                Double basicSalary = Json.getDouble(
                        body,
                        "basicSalary");

                // ------------------------------------------------
                // Validate required fields
                // ------------------------------------------------

                if (employeeId == null) {

                    throw new IllegalArgumentException(
                            "Employee is required.");
                }

                if (month == null
                        || month < 1
                        || month > 12) {

                    throw new IllegalArgumentException(
                            "Payroll month must be between 1 and 12.");
                }

                if (year == null
                        || year < 2000
                        || year > 2100) {

                    throw new IllegalArgumentException(
                            "Payroll year must be between 2000 and 2100.");
                }

                if (basicSalary == null
                        || basicSalary < 0) {

                    throw new IllegalArgumentException(
                            "Basic salary must be zero or greater.");
                }

                // ------------------------------------------------
                // Optional overtime rate
                // ------------------------------------------------

                double overtimeRate = 0;

                if (body.get(
                        "overtimeRatePerHour") != null) {

                    Double value = Json.getDouble(
                            body,
                            "overtimeRatePerHour");

                    if (value != null) {

                        if (value < 0) {

                            throw new IllegalArgumentException(
                                    "Overtime rate cannot be negative.");
                        }

                        overtimeRate = value;
                    }
                }

                // ------------------------------------------------
                // IMPORTANT:
                // Read arrays as actual List<Map>
                // ------------------------------------------------

                List<Map<String, Object>> allowances = Json.getObjectList(
                        body,
                        "allowances");

                List<Map<String, Object>> deductions = Json.getObjectList(
                        body,
                        "deductions");

                // ------------------------------------------------
                // Validate allowance rows
                // ------------------------------------------------

                for (Map<String, Object> allowance : allowances) {

                    String type = Json.getString(
                            allowance,
                            "type");

                    Double amount = Json.getDouble(
                            allowance,
                            "amount");

                    if (type == null
                            || type.isBlank()) {

                        throw new IllegalArgumentException(
                                "Allowance type is required.");
                    }

                    if (amount == null
                            || amount < 0) {

                        throw new IllegalArgumentException(
                                "Allowance amount must be zero or greater.");
                    }
                }

                // ------------------------------------------------
                // Validate deduction rows
                // ------------------------------------------------

                for (Map<String, Object> deduction : deductions) {

                    String type = Json.getString(
                            deduction,
                            "type");

                    Double amount = Json.getDouble(
                            deduction,
                            "amount");

                    if (type == null
                            || type.isBlank()) {

                        throw new IllegalArgumentException(
                                "Deduction type is required.");
                    }

                    if (amount == null
                            || amount < 0) {

                        throw new IllegalArgumentException(
                                "Deduction amount must be zero or greater.");
                    }
                }

                // ------------------------------------------------
                // Process payroll
                // ------------------------------------------------

                int payrollId = payrollDAO.processPayroll(
                        employeeId,
                        month,
                        year,
                        basicSalary,
                        allowances,
                        deductions,
                        overtimeRate);

                // ------------------------------------------------
                // Audit log
                // ------------------------------------------------

                auditLogDAO.record(
                        session.userId,
                        "CREATE",
                        "PAYROLL",
                        "Processed payroll for employee "
                                + employeeId
                                + ", "
                                + month
                                + "/"
                                + year);

                // ------------------------------------------------
                // Notify employee
                // ------------------------------------------------

                Integer userId = userAccountDAO
                        .findUserIdByEmployeeId(
                                employeeId);

                if (userId != null) {

                    notificationDAO.create(
                            userId,
                            "PAYROLL_AVAILABLE",
                            "Your payslip for "
                                    + month
                                    + "/"
                                    + year
                                    + " is now available.");
                }

                // ------------------------------------------------
                // Success response
                // ------------------------------------------------

                HttpUtil.sendJson(
                        exchange,
                        201,
                        Map.of(
                                "payrollId",
                                payrollId));

            } catch (SQLException e) {

                AuthController.handleDbError(
                        exchange,
                        e);

            } catch (Exception e) {

                e.printStackTrace();

                HttpUtil.sendError(
                        exchange,
                        400,
                        "Invalid payroll data: "
                                + e.getMessage());
            }
        };
    }

    // ============================================================
    // VERIFY PAYROLL
    // ============================================================

    public HttpHandler verify() {

        return exchange -> {

            if (HttpUtil.handlePreflight(exchange)) {
                return;
            }

            SessionManager.Session session = Security.requireRole(
                    exchange,
                    MANAGE_ROLE);

            if (session == null) {
                return;
            }

            int id = extractIdBeforeSuffix(
                    exchange,
                    "/verify");

            try {

                payrollDAO.verify(id);

                auditLogDAO.record(
                        session.userId,
                        "VERIFY",
                        "PAYROLL",
                        "Verified payroll ID "
                                + id);

                HttpUtil.sendJson(
                        exchange,
                        200,
                        Map.of(
                                "message",
                                "Payroll verified"));

            } catch (SQLException e) {

                AuthController.handleDbError(
                        exchange,
                        e);
            }
        };
    }

    // ============================================================
    // MARK PAYROLL PAID
    // ============================================================

    public HttpHandler markPaid() {

        return exchange -> {

            if (HttpUtil.handlePreflight(exchange)) {
                return;
            }

            SessionManager.Session session = Security.requireRole(
                    exchange,
                    MANAGE_ROLE);

            if (session == null) {
                return;
            }

            int id = extractIdBeforeSuffix(
                    exchange,
                    "/pay");

            try {

                payrollDAO.markPaid(id);

                auditLogDAO.record(
                        session.userId,
                        "PAY",
                        "PAYROLL",
                        "Marked payroll ID "
                                + id
                                + " as paid");

                HttpUtil.sendJson(
                        exchange,
                        200,
                        Map.of(
                                "message",
                                "Payroll marked as paid"));

            } catch (SQLException e) {

                AuthController.handleDbError(
                        exchange,
                        e);
            }
        };
    }

    // ============================================================
    // SUMMARY
    // ============================================================

    public HttpHandler summary() {

        return exchange -> {

            if (HttpUtil.handlePreflight(exchange)) {
                return;
            }

            SessionManager.Session session = Security.requireRole(
                    exchange,
                    READ_ROLES);

            if (session == null) {
                return;
            }

            try {

                Map<String, String> query = HttpUtil.parseQuery(exchange);

                Integer month = query.containsKey("month")
                        ? Integer.parseInt(
                                query.get("month"))
                        : null;

                Integer year = query.containsKey("year")
                        ? Integer.parseInt(
                                query.get("year"))
                        : null;

                HttpUtil.sendJson(
                        exchange,
                        200,
                        payrollDAO.summary(
                                month,
                                year));

            } catch (SQLException e) {

                AuthController.handleDbError(
                        exchange,
                        e);

            } catch (Exception e) {

                HttpUtil.sendError(
                        exchange,
                        400,
                        "Invalid payroll summary filter: "
                                + e.getMessage());
            }
        };
    }

    // ============================================================
    // ID EXTRACTION
    // ============================================================

    private int extractIdBeforeSuffix(
            com.sun.net.httpserver.HttpExchange exchange,
            String suffix) {

        String path = exchange.getRequestURI()
                .getPath();

        String trimmed = path.endsWith(suffix)
                ? path.substring(
                        0,
                        path.length()
                                - suffix.length())
                : path;

        String[] parts = trimmed.split("/");

        return Integer.parseInt(
                parts[parts.length - 1]);
    }
}