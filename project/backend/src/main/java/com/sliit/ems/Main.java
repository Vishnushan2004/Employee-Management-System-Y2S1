package com.sliit.ems;

import com.sliit.ems.config.DBConfig;
import com.sliit.ems.config.Database;
import com.sliit.ems.controller.*;
import com.sliit.ems.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Application entry point. Starts a plain Java SE HTTP server (no external
 * web framework) and registers all API routes.
 *
 * Run with:  java -cp "out;lib/*" com.sliit.ems.Main   (Windows)
 *            java -cp "out:lib/*" com.sliit.ems.Main   (macOS/Linux)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // Fail fast with a clear message if the DB isn't reachable, rather than
        // starting a server that will error on every request.
        try (Connection ignored = Database.getConnection()) {
            System.out.println("Connected to SQL Server successfully.");
        } catch (Exception e) {
            System.err.println("Could not connect to SQL Server: " + e.getMessage());
            System.err.println("Check config.properties and make sure SQL Server is running.");
            return;
        }

        int port = DBConfig.getServerPort();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        AuthController auth = new AuthController();
        EmployeeController employees = new EmployeeController();
        LeaveController leaves = new LeaveController();
        AttendanceController attendance = new AttendanceController();
        DashboardController dashboard = new DashboardController();
        LookupController lookups = new LookupController();
        NotificationController notifications = new NotificationController();
        AuditLogController auditLogs = new AuditLogController();
        ShiftController shifts = new ShiftController();
        PayrollController payroll = new PayrollController();
        RecruitmentController recruitment = new RecruitmentController();
        PerformanceController performance = new PerformanceController();
        TrainingController training = new TrainingController();
        CareerController career = new CareerController();
        AdminController admin = new AdminController();
        ApplicantPortalController applicantPortal = new ApplicantPortalController();

        // ---- Auth ----
        server.createContext("/api/auth/login", auth.login());
        server.createContext("/api/auth/logout", auth.logout());
        server.createContext("/api/auth/register", auth.register());

        // ---- Public (no login required) ----
        server.createContext("/api/public/vacancies", applicantPortal.publicOpenVacancies());

        // ---- Outside-user (job applicant) portal ----
        server.createContext("/api/applicant/profile", methodRouter(applicantPortal.getProfile(), applicantPortal.updateProfile()));
        server.createContext("/api/applicant/applications", methodRouter(applicantPortal.listMyApplications(), applicantPortal.apply()));
        server.createContext("/api/applicant/applications/", suffixRouter(Map.of(
            "/cv", applicantPortal.cv(),
            "/interview", applicantPortal.interviewForApplication()
        )));

        // ---- Dashboard ----
        server.createContext("/api/dashboard", dashboard.get());

        // ---- Employees ----
        // Order matters: more specific paths are registered; HttpServer routes by longest prefix match.
        server.createContext("/api/employees/me", methodRouter(employees.getMe(), employees.updateOwnProfile()));
        server.createContext("/api/employees", methodRouter(employees.list(), employees.create()));
        server.createContext("/api/employees/", employeeByIdRouter(employees));

        // ---- Leaves ----
        server.createContext("/api/leaves", methodRouter(leaves.list(), leaves.apply()));
        server.createContext("/api/leaves/", suffixRouter(Map.of(
            "/approve", leaves.approve(),
            "/reject", leaves.reject(),
            "/cancel", leaves.cancel()
        )));

        // ---- Attendance ----
        server.createContext("/api/attendance/check-in", attendance.checkIn());
        server.createContext("/api/attendance/check-out", attendance.checkOut());
        server.createContext("/api/attendance", methodRouter(attendance.list(), null));
        server.createContext("/api/attendance/correct/", attendance.correct());

        // ---- Lookups ----
        server.createContext("/api/departments", lookups.departments());
        server.createContext("/api/designations", lookups.designations());
        server.createContext("/api/roles", methodRouter(lookups.roles(), admin.createRole()));

        // ---- Notifications ----
        server.createContext("/api/notifications", notifications.list());
        server.createContext("/api/notifications/", suffixRouter(Map.of("/read", notifications.markRead())));

        // ---- Audit logs ----
        server.createContext("/api/audit-logs", auditLogs.list());

        // ---- Shifts ----
        server.createContext("/api/shifts", methodRouter(shifts.listShifts(), shifts.createShift()));
        server.createContext("/api/shifts/", shifts.updateShift());
        server.createContext("/api/shift-assignments", methodRouter(shifts.listAssignments(), shifts.assignShift()));

        // ---- Payroll ----
        server.createContext("/api/payroll/summary", payroll.summary());
        server.createContext("/api/payroll/detail/", payroll.detail());
        server.createContext("/api/payroll", methodRouter(payroll.list(), payroll.process()));
        server.createContext("/api/payroll/", suffixRouter(Map.of(
            "/verify", payroll.verify(),
            "/pay", payroll.markPaid()
        )));

        // ---- Recruitment ----
        server.createContext("/api/vacancies", methodRouter(recruitment.listVacancies(), recruitment.createVacancy()));
        server.createContext("/api/vacancies/", suffixRouter(Map.of("/status", recruitment.updateVacancyStatus())));
        server.createContext("/api/applicants", methodRouter(recruitment.listApplicants(), recruitment.createApplicant()));
        server.createContext("/api/applications", methodRouter(recruitment.listApplications(), recruitment.createApplication()));
        server.createContext("/api/applications/", suffixRouter(Map.of(
            "/status", recruitment.updateApplicationStatus(),
            "/cv", recruitment.downloadCvForHr()
        )));
        server.createContext("/api/interviews", methodRouter(recruitment.listInterviews(), recruitment.scheduleInterview()));
        server.createContext("/api/interviews/", suffixRouter(Map.of("/result", recruitment.recordInterviewResult())));

        // ---- Performance ----
        server.createContext("/api/performance/goals", methodRouter(performance.listGoals(), performance.createGoal()));
        server.createContext("/api/performance/goals/", performance.updateGoal());
        server.createContext("/api/performance/reviews", methodRouter(performance.listReviews(), performance.createReview()));

        // ---- Training / Certification ----
        server.createContext("/api/training", methodRouter(training.listTraining(), training.createTraining()));
        server.createContext("/api/training/", suffixRouter(Map.of(
            "/assign", training.assignTraining(),
            "/attendees", training.attendees()
        )));
        server.createContext("/api/certifications", methodRouter(training.listCertifications(), training.createCertification()));

        // ---- Promotion / Transfer ----
        server.createContext("/api/promotions", methodRouter(career.listPromotions(), career.createPromotion()));
        server.createContext("/api/transfers", methodRouter(career.listTransfers(), career.createTransfer()));

        // ---- Admin: users / roles / permissions ----
        server.createContext("/api/users", methodRouter(admin.listUsers(), admin.createUser()));
        server.createContext("/api/users/", suffixRouter(Map.of(
            "/status", admin.setAccountStatus(),
            "/role", admin.changeRole()
        )));
        server.createContext("/api/permissions", methodRouter(admin.listPermissions(), admin.createPermission()));
        server.createContext("/api/permissions/role/", admin.permissionsForRole());
        server.createContext("/api/role-permissions", methodRouter(null, admin.assignPermission()));

        server.start();
        System.out.println("Employee Management System backend running on http://localhost:" + port);
    }

    /** Dispatches GET to one handler and POST/PUT to another on the same path (JDK HttpServer has no verb routing). */
    private static HttpHandler methodRouter(HttpHandler getHandler, HttpHandler postHandler) {
        return exchange -> {
            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                HttpUtil.handlePreflight(exchange);
                return;
            }
            if ("GET".equalsIgnoreCase(method) && getHandler != null) {
                getHandler.handle(exchange);
            } else if (("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) && postHandler != null) {
                postHandler.handle(exchange);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed on this endpoint.");
            }
        };
    }

    /** Routes /api/employees/{id} (GET/PUT) and /api/employees/{id}/deactivate. */
    private static HttpHandler employeeByIdRouter(EmployeeController employees) {
        return exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                HttpUtil.handlePreflight(exchange);
                return;
            }
            if (path.endsWith("/deactivate")) {
                employees.deactivate().handle(exchange);
            } else if ("GET".equalsIgnoreCase(method)) {
                employees.getById().handle(exchange);
            } else if ("PUT".equalsIgnoreCase(method)) {
                employees.update().handle(exchange);
            } else {
                HttpUtil.sendError(exchange, 405, "Method not allowed on this endpoint.");
            }
        };
    }

    /**
     * Generic dispatcher for "/{id}/action" style sub-paths registered under one context
     * (e.g. /api/leaves/{id}/approve, /api/payroll/{id}/verify). Matches by path suffix.
     */
    private static HttpHandler suffixRouter(Map<String, HttpHandler> actionsBySuffix) {
        // LinkedHashMap iteration order preserved from Map.of is not guaranteed, but suffixes
        // here never overlap as prefixes of one another, so match order does not matter.
        Map<String, HttpHandler> actions = new LinkedHashMap<>(actionsBySuffix);
        return exchange -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.handlePreflight(exchange);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            for (Map.Entry<String, HttpHandler> entry : actions.entrySet()) {
                if (path.endsWith(entry.getKey())) {
                    entry.getValue().handle(exchange);
                    return;
                }
            }
            HttpUtil.sendError(exchange, 404, "Unknown action for this endpoint.");
        };
    }
}
