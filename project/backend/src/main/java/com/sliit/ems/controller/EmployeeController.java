package com.sliit.ems.controller;

import com.sliit.ems.dao.EmployeeDAO;
import com.sliit.ems.dao.AuditLogDAO;
import com.sliit.ems.model.Employee;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeController {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String[] STAFF_ROLES = {
        "HR_MANAGER", "ADMINISTRATIVE_OFFICER", "IT_COORDINATOR", "OPERATIONS_MANAGER", "FINANCE_PAYROLL_MANAGER"
    };
    private static final String[] EDIT_ROLES = { "HR_MANAGER", "ADMINISTRATIVE_OFFICER" };

    /** GET /api/employees?search=&departmentId=  - directory/search, any staff role. Employees see nothing here (use /me). */
    public HttpHandler list() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = com.sliit.ems.util.Security.requireRole(exchange, STAFF_ROLES);
            if (session == null) return;
            try {
                Map<String, String> query = HttpUtil.parseQuery(exchange);
                String search = query.get("search");
                Integer departmentId = query.containsKey("departmentId") ? Integer.parseInt(query.get("departmentId")) : null;
                List<Employee> employees = employeeDAO.findAll(search, departmentId);
                HttpUtil.sendJson(exchange, 200, employees.stream().map(this::toMap).toList());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** GET /api/employees/{id} - staff roles can view any employee; an employee can only view themselves. */
    public HttpHandler getById() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = com.sliit.ems.util.Security.requireAuth(exchange);
            if (session == null) return;
            int id = HttpUtil.extractIdFromPath(exchange);
            boolean isStaff = List.of(STAFF_ROLES).contains(session.roleName);
            if (!isStaff && session.employeeId != id) {
                HttpUtil.sendError(exchange, 403, "You can only view your own profile.");
                return;
            }
            try {
                Employee employee = employeeDAO.findById(id);
                if (employee == null) {
                    HttpUtil.sendError(exchange, 404, "Employee not found.");
                    return;
                }
                HttpUtil.sendJson(exchange, 200, toMap(employee));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** GET /api/employees/me - convenience endpoint for the logged-in employee's own profile. */
    public HttpHandler getMe() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = com.sliit.ems.util.Security.requireAuth(exchange);
            if (session == null) return;
            if (session.employeeId == 0) {
                HttpUtil.sendError(exchange, 403, "This endpoint is only available to staff/employee accounts.");
                return;
            }
            try {
                Employee employee = employeeDAO.findById(session.employeeId);
                HttpUtil.sendJson(exchange, 200, toMap(employee));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** POST /api/employees - HR/Admin only. */
    public HttpHandler create() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = com.sliit.ems.util.Security.requireRole(exchange, EDIT_ROLES);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                Employee emp = fromMap(body, null);

                if (emp.employeeNumber == null || emp.employeeNumber.isBlank()) {
                    HttpUtil.sendError(exchange, 400, "Employee number is required.");
                    return;
                }
                if (employeeDAO.employeeNumberExists(emp.employeeNumber)) {
                    HttpUtil.sendError(exchange, 409, "Employee number already exists.");
                    return;
                }
                int newId = employeeDAO.insert(emp);
                auditLogDAO.record(session.userId, "CREATE", "EMPLOYEE", "Created employee " + emp.employeeNumber);
                HttpUtil.sendJson(exchange, 201, Map.of("employeeId", newId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException | NullPointerException e) {
                HttpUtil.sendError(exchange, 400, "Invalid or missing employee fields: " + e.getMessage());
            }
        };
    }

    /** PUT /api/employees/{id} - HR/Admin only, full record edit. */
    public HttpHandler update() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = com.sliit.ems.util.Security.requireRole(exchange, EDIT_ROLES);
            if (session == null) return;
            int id = HttpUtil.extractIdFromPath(exchange);
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                Employee emp = fromMap(body, id);
                employeeDAO.update(emp);
                auditLogDAO.record(session.userId, "UPDATE", "EMPLOYEE", "Updated employee ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Employee updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException | NullPointerException e) {
                HttpUtil.sendError(exchange, 400, "Invalid or missing employee fields: " + e.getMessage());
            }
        };
    }

    /** PUT /api/employees/me - any employee can update their own permitted personal fields only. */
    public HttpHandler updateOwnProfile() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = com.sliit.ems.util.Security.requireAuth(exchange);
            if (session == null) return;
            if (session.employeeId == 0) {
                HttpUtil.sendError(exchange, 403, "This endpoint is only available to staff/employee accounts.");
                return;
            }
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                String phone = Json.getString(body, "phoneNumber");
                String street = Json.getString(body, "street");
                String city = Json.getString(body, "city");
                String postalCode = Json.getString(body, "postalCode");
                employeeDAO.updateOwnProfile(session.employeeId, phone, street, city, postalCode);
                auditLogDAO.record(session.userId, "UPDATE", "EMPLOYEE_SELF_SERVICE", "Employee updated own contact details");
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Profile updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** DELETE /api/employees/{id} - HR only, soft-delete (deactivate), never a hard delete. */
    public HttpHandler deactivate() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = com.sliit.ems.util.Security.requireRole(exchange, "HR_MANAGER");
            if (session == null) return;
            int id = extractIdBeforeSuffix(exchange, "/deactivate");
            try {
                employeeDAO.deactivate(id);
                auditLogDAO.record(session.userId, "DEACTIVATE", "EMPLOYEE", "Deactivated employee ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Employee deactivated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    private Employee fromMap(Map<String, Object> body, Integer existingId) {
        Employee e = new Employee();
        e.employeeId = existingId == null ? 0 : existingId;
        e.employeeNumber = Json.getString(body, "employeeNumber");
        e.firstName = requireNonBlank(Json.getString(body, "firstName"), "firstName");
        e.middleName = Json.getString(body, "middleName");
        e.lastName = requireNonBlank(Json.getString(body, "lastName"), "lastName");
        e.dateOfBirth = LocalDate.parse(requireNonBlank(Json.getString(body, "dateOfBirth"), "dateOfBirth"));
        e.gender = requireNonBlank(Json.getString(body, "gender"), "gender");
        e.phoneNumber = requireNonBlank(Json.getString(body, "phoneNumber"), "phoneNumber");
        if (!com.sliit.ems.util.Validation.isValidPhone(e.phoneNumber)) {
            throw new IllegalArgumentException("phoneNumber must contain only digits, spaces, '+' or '-'");
        }
        e.street = Json.getString(body, "street");
        e.city = Json.getString(body, "city");
        e.postalCode = Json.getString(body, "postalCode");
        e.departmentId = Json.getInt(body, "departmentId");
        e.designationId = Json.getInt(body, "designationId");
        e.employmentType = requireNonBlank(Json.getString(body, "employmentType"), "employmentType");
        e.employmentStatus = Json.getString(body, "employmentStatus") == null ? "ACTIVE" : Json.getString(body, "employmentStatus");
        e.employmentStartDate = LocalDate.parse(requireNonBlank(Json.getString(body, "employmentStartDate"), "employmentStartDate"));
        e.workLocation = Json.getString(body, "workLocation");
        e.supervisorEmployeeId = Json.getInt(body, "supervisorEmployeeId");
        return e;
    }

    private int extractIdBeforeSuffix(com.sun.net.httpserver.HttpExchange exchange, String suffix) {
        String path = exchange.getRequestURI().getPath();
        String trimmed = path.endsWith(suffix) ? path.substring(0, path.length() - suffix.length()) : path;
        String[] parts = trimmed.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value;
    }

    private Map<String, Object> toMap(Employee e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("employeeId", e.employeeId);
        m.put("employeeNumber", e.employeeNumber);
        m.put("firstName", e.firstName);
        m.put("middleName", e.middleName);
        m.put("lastName", e.lastName);
        m.put("fullName", e.fullName());
        m.put("dateOfBirth", e.dateOfBirth == null ? null : e.dateOfBirth.toString());
        m.put("gender", e.gender);
        m.put("phoneNumber", e.phoneNumber);
        m.put("street", e.street);
        m.put("city", e.city);
        m.put("postalCode", e.postalCode);
        m.put("departmentId", e.departmentId);
        m.put("departmentName", e.departmentName);
        m.put("designationId", e.designationId);
        m.put("designationName", e.designationName);
        m.put("employmentType", e.employmentType);
        m.put("employmentStatus", e.employmentStatus);
        m.put("employmentStartDate", e.employmentStartDate == null ? null : e.employmentStartDate.toString());
        m.put("workLocation", e.workLocation);
        m.put("supervisorEmployeeId", e.supervisorEmployeeId);
        m.put("annualLeaveBalance", e.annualLeaveBalance);
        m.put("casualLeaveBalance", e.casualLeaveBalance);
        m.put("sickLeaveBalance", e.sickLeaveBalance);
        return m;
    }
}
