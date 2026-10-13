package com.sliit.ems.controller.finance;

import com.sliit.ems.controller.AuthController;

import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.dao.finance.PayrollDAO;
import com.sliit.ems.dao.it.UserAccountDAO;
import com.sliit.ems.dao.NotificationDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PayrollController {

        // ============================================================
        // DAOs
        // ============================================================

        private final PayrollDAO payrollDAO = new PayrollDAO();
        private final AuditLogDAO auditLogDAO = new AuditLogDAO();
        private final UserAccountDAO userAccountDAO = new UserAccountDAO();
        private final NotificationDAO notificationDAO = new NotificationDAO();

        // ============================================================
        // ROLES
        // ============================================================

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
                                                "Invalid payroll request: "
                                                                + e.getMessage());
                        }
                };
        }

        // ============================================================
        // PAYROLL DETAIL
        // GET /api/payroll/detail/{id}
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

                        int id = HttpUtil.extractIdFromPath(exchange);

                        if (id <= 0) {

                                HttpUtil.sendError(
                                                exchange,
                                                400,
                                                "Invalid payroll ID.");

                                return;
                        }

                        try {

                                Map<String, Object> result = new LinkedHashMap<>();

                                // Get allowances
                                result.put(
                                                "allowances",
                                                payrollDAO.findAllowances(id));

                                // Get deductions
                                result.put(
                                                "deductions",
                                                payrollDAO.findDeductions(id));

                                // Get payslip
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
                                                "Invalid payroll request: "
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

                        // Only Finance & Payroll Manager can process payroll
                        SessionManager.Session session = Security.requireRole(
                                        exchange,
                                        MANAGE_ROLE);

                        if (session == null) {
                                return;
                        }

                        try {

                                // ====================================================
                                // READ REQUEST BODY
                                // ====================================================

                                Map<String, Object> body = HttpUtil.readJsonBody(exchange);

                                // ====================================================
                                // BASIC PAYROLL DATA
                                // ====================================================

                                int employeeId = Json.getInt(
                                                body,
                                                "employeeId");

                                int month = Json.getInt(
                                                body,
                                                "payrollMonth");

                                int year = Json.getInt(
                                                body,
                                                "payrollYear");

                                double basicSalary = Json.getDouble(
                                                body,
                                                "basicSalary");

                                // ====================================================
                                // OVERTIME RATE
                                // ====================================================

                                double overtimeRate = 0;

                                if (body.get("overtimeRatePerHour") != null) {

                                        overtimeRate = Json.getDouble(
                                                        body,
                                                        "overtimeRatePerHour");
                                }

                                // ====================================================
                                // VALIDATE MONTH
                                // ====================================================

                                if (month < 1 || month > 12) {

                                        HttpUtil.sendError(
                                                        exchange,
                                                        400,
                                                        "Payroll month must be between 1 and 12.");

                                        return;
                                }

                                // ====================================================
                                // VALIDATE BASIC SALARY
                                // ====================================================

                                com.sliit.ems.util.Validation
                                                .requireNonNegative(
                                                                basicSalary,
                                                                "Basic salary");

                                // ====================================================
                                // VALIDATE OVERTIME RATE
                                // ====================================================

                                com.sliit.ems.util.Validation
                                                .requireNonNegative(
                                                                overtimeRate,
                                                                "Overtime rate per hour");

                                // ====================================================
                                // READ ALLOWANCES
                                // ====================================================

                                List<Map<String, Object>> allowances = parsePayrollItems(
                                                body.get("allowances"),
                                                "allowances");

                                // ====================================================
                                // READ DEDUCTIONS
                                // ====================================================

                                List<Map<String, Object>> deductions = parsePayrollItems(
                                                body.get("deductions"),
                                                "deductions");

                                // ====================================================
                                // VALIDATE ALLOWANCES
                                // ====================================================

                                for (Map<String, Object> allowance : allowances) {

                                        if (allowance == null) {
                                                continue;
                                        }

                                        Object amount = allowance.get("amount");

                                        if (!(amount instanceof Number)) {

                                                HttpUtil.sendError(
                                                                exchange,
                                                                400,
                                                                "Allowance amount must be a number.");

                                                return;
                                        }

                                        com.sliit.ems.util.Validation
                                                        .requireNonNegative(
                                                                        ((Number) amount)
                                                                                        .doubleValue(),
                                                                        "Allowance amount");
                                }

                                // ====================================================
                                // VALIDATE DEDUCTIONS
                                // ====================================================

                                for (Map<String, Object> deduction : deductions) {

                                        if (deduction == null) {
                                                continue;
                                        }

                                        Object amount = deduction.get("amount");

                                        if (!(amount instanceof Number)) {

                                                HttpUtil.sendError(
                                                                exchange,
                                                                400,
                                                                "Deduction amount must be a number.");

                                                return;
                                        }

                                        com.sliit.ems.util.Validation
                                                        .requireNonNegative(
                                                                        ((Number) amount)
                                                                                        .doubleValue(),
                                                                        "Deduction amount");
                                }

                                // ====================================================
                                // PROCESS PAYROLL IN DATABASE
                                // ====================================================

                                int payrollId = payrollDAO.processPayroll(
                                                employeeId,
                                                month,
                                                year,
                                                basicSalary,
                                                allowances,
                                                deductions,
                                                overtimeRate);

                                // ====================================================
                                // AUDIT LOG
                                // ====================================================

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

                                // ====================================================
                                // NOTIFICATION
                                // ====================================================

                                Integer userId = userAccountDAO.findUserIdByEmployeeId(
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

                                // ====================================================
                                // SUCCESS RESPONSE
                                // ====================================================

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
        // PARSE ALLOWANCES / DEDUCTIONS
        // ============================================================

        /**
         * Reads allowances or deductions from the already-parsed
         * JSON request body.
         *
         * Expected format:
         *
         * "allowances": [
         * {
         * "type": "Transport",
         * "amount": 10000
         * }
         * ]
         *
         * "deductions": [
         * {
         * "type": "EPF Employee Share",
         * "amount": 5000
         * }
         * ]
         *
         * IMPORTANT:
         * HttpUtil.readJsonBody() already converts the JSON array
         * into a Java List.
         *
         * Therefore we DO NOT call Json.parse() here.
         */

        private List<Map<String, Object>> parsePayrollItems(
                        Object value,
                        String fieldName) {

                // ========================================================
                // FIELD NOT PROVIDED
                // ========================================================

                if (value == null) {
                        return new ArrayList<>();
                }

                // ========================================================
                // JSON ARRAY ALREADY PARSED AS JAVA LIST
                // ========================================================

                if (value instanceof List<?>) {

                        List<?> rawList = (List<?>) value;

                        List<Map<String, Object>> result = new ArrayList<>();

                        for (Object item : rawList) {

                                // Every array item must be an object
                                if (!(item instanceof Map<?, ?>)) {

                                        throw new IllegalArgumentException(
                                                        fieldName
                                                                        + " must contain JSON objects.");
                                }

                                Map<?, ?> rawMap = (Map<?, ?>) item;

                                Map<String, Object> itemMap = new LinkedHashMap<>();

                                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {

                                        // JSON object keys must be strings
                                        if (!(entry.getKey() instanceof String)) {

                                                throw new IllegalArgumentException(
                                                                fieldName
                                                                                + " contains an invalid object.");
                                        }

                                        itemMap.put(
                                                        (String) entry.getKey(),
                                                        entry.getValue());
                                }

                                result.add(itemMap);
                        }

                        return result;
                }

                // ========================================================
                // INVALID TYPE
                // ========================================================

                /*
                 * DO NOT try Json.parse() here.
                 *
                 * HttpUtil.readJsonBody() has already parsed the
                 * complete JSON request.
                 *
                 * If the frontend sends:
                 *
                 * "allowances": [...]
                 *
                 * then body.get("allowances") is already a List.
                 */

                throw new IllegalArgumentException(
                                fieldName
                                                + " must be a JSON array.");
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

                        if (id <= 0) {

                                HttpUtil.sendError(
                                                exchange,
                                                400,
                                                "Invalid payroll ID.");

                                return;
                        }

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
        // MARK PAYROLL AS PAID
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

                        if (id <= 0) {

                                HttpUtil.sendError(
                                                exchange,
                                                400,
                                                "Invalid payroll ID.");

                                return;
                        }

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
        // PAYROLL SUMMARY
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
                                                "Invalid summary request: "
                                                                + e.getMessage());
                        }
                };
        }

        // ============================================================
        // EXTRACT ID BEFORE SUFFIX
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

                if (parts.length == 0) {

                        throw new IllegalArgumentException(
                                        "Invalid payroll ID.");
                }

                try {

                        return Integer.parseInt(
                                        parts[parts.length - 1]);

                } catch (NumberFormatException e) {

                        throw new IllegalArgumentException(
                                        "Invalid payroll ID.");
                }
        }
}