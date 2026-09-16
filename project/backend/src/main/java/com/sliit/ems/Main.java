package com.sliit.ems;

import com.sliit.ems.config.DBConfig;
import com.sliit.ems.config.Database;

import com.sliit.ems.controller.AdminController;
import com.sliit.ems.controller.AttendanceController;
import com.sliit.ems.controller.AuditLogController;
import com.sliit.ems.controller.AuthController;
import com.sliit.ems.controller.CareerController;
import com.sliit.ems.controller.DashboardController;
import com.sliit.ems.controller.EmployeeController;
import com.sliit.ems.controller.LeaveController;
import com.sliit.ems.controller.LookupController;
import com.sliit.ems.controller.NotificationController;
import com.sliit.ems.controller.PayrollController;
import com.sliit.ems.controller.PerformanceController;
import com.sliit.ems.controller.RecruitmentController;
import com.sliit.ems.controller.ShiftController;
import com.sliit.ems.controller.TrainingController;

import com.sliit.ems.util.HttpUtil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.sql.Connection;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args)
            throws Exception {

        // ========================================================
        // DATABASE CONNECTION TEST
        // ========================================================

        try (Connection ignored = Database.getConnection()) {

            System.out.println(
                    "Connected to SQL Server successfully.");

        } catch (Exception e) {

            System.err.println(
                    "Could not connect to SQL Server: "
                            + e.getMessage());

            System.err.println(
                    "Check config.properties and make sure SQL Server is running.");

            e.printStackTrace();

            return;
        }

        // ========================================================
        // SERVER
        // ========================================================

        int port = DBConfig.getServerPort();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(port),
                0);

        server.setExecutor(
                Executors.newFixedThreadPool(8));

        // ========================================================
        // CONTROLLERS
        // ========================================================

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

        // ========================================================
        // AUTH
        // ========================================================

        server.createContext(
                "/api/auth/login",
                auth.login());

        server.createContext(
                "/api/auth/logout",
                auth.logout());

        // ========================================================
        // DASHBOARD
        // ========================================================

        server.createContext(
                "/api/dashboard",
                dashboard.get());

        // ========================================================
        // EMPLOYEES
        // ========================================================

        server.createContext(
                "/api/employees/me",
                methodRouter(
                        employees.getMe(),
                        employees.updateOwnProfile()));

        server.createContext(
                "/api/employees",
                methodRouter(
                        employees.list(),
                        employees.create()));

        server.createContext(
                "/api/employees/",
                employeeByIdRouter(employees));

        // ========================================================
        // LEAVES
        // ========================================================

        server.createContext(
                "/api/leaves",
                methodRouter(
                        leaves.list(),
                        leaves.apply()));

        server.createContext(
                "/api/leaves/",
                suffixRouter(
                        Map.of(
                                "/approve",
                                leaves.approve(),

                                "/reject",
                                leaves.reject(),

                                "/cancel",
                                leaves.cancel())));

        // ========================================================
        // ATTENDANCE
        // ========================================================

        server.createContext(
                "/api/attendance/check-in",
                attendance.checkIn());

        server.createContext(
                "/api/attendance/check-out",
                attendance.checkOut());

        server.createContext(
                "/api/attendance",
                methodRouter(
                        attendance.list(),
                        null));

        server.createContext(
                "/api/attendance/correct/",
                attendance.correct());

        // ========================================================
        // LOOKUPS
        // ========================================================

        server.createContext(
                "/api/departments",
                lookups.departments());

        server.createContext(
                "/api/designations",
                lookups.designations());

        server.createContext(
                "/api/roles",
                methodRouter(
                        lookups.roles(),
                        admin.createRole()));

        // ========================================================
        // NOTIFICATIONS
        // ========================================================

        server.createContext(
                "/api/notifications",
                notifications.list());

        server.createContext(
                "/api/notifications/",
                suffixRouter(
                        Map.of(
                                "/read",
                                notifications.markRead())));

        // ========================================================
        // AUDIT LOGS
        // ========================================================

        server.createContext(
                "/api/audit-logs",
                auditLogs.list());

        // ========================================================
        // SHIFTS
        // ========================================================

        server.createContext(
                "/api/shifts",
                methodRouter(
                        shifts.listShifts(),
                        shifts.createShift()));

        server.createContext(
                "/api/shifts/",
                shifts.updateShift());

        server.createContext(
                "/api/shift-assignments",
                methodRouter(
                        shifts.listAssignments(),
                        shifts.assignShift()));

        // ========================================================
        // PAYROLL
        // ========================================================

        server.createContext(
                "/api/payroll/summary",
                payroll.summary());

        server.createContext(
                "/api/payroll/detail/",
                payroll.detail());

        server.createContext(
                "/api/payroll",
                methodRouter(
                        payroll.list(),
                        payroll.process()));

        server.createContext(
                "/api/payroll/",
                suffixRouter(
                        Map.of(
                                "/verify",
                                payroll.verify(),

                                "/pay",
                                payroll.markPaid())));

        // ========================================================
        // RECRUITMENT
        // ========================================================

        server.createContext(
                "/api/vacancies",
                methodRouter(
                        recruitment.listVacancies(),
                        recruitment.createVacancy()));

        server.createContext(
                "/api/vacancies/",
                suffixRouter(
                        Map.of(
                                "/status",
                                recruitment.updateVacancyStatus())));

        server.createContext(
                "/api/applicants",
                methodRouter(
                        recruitment.listApplicants(),
                        recruitment.createApplicant()));

        server.createContext(
                "/api/applications",
                methodRouter(
                        recruitment.listApplications(),
                        recruitment.createApplication()));

        server.createContext(
                "/api/applications/",
                suffixRouter(
                        Map.of(
                                "/status",
                                recruitment.updateApplicationStatus())));

        server.createContext(
                "/api/interviews",
                methodRouter(
                        recruitment.listInterviews(),
                        recruitment.scheduleInterview()));

        server.createContext(
                "/api/interviews/",
                suffixRouter(
                        Map.of(
                                "/result",
                                recruitment.recordInterviewResult())));

        // ========================================================
        // PERFORMANCE
        // ========================================================

        server.createContext(
                "/api/performance/goals",
                methodRouter(
                        performance.listGoals(),
                        performance.createGoal()));

        server.createContext(
                "/api/performance/goals/",
                performance.updateGoal());

        server.createContext(
                "/api/performance/reviews",
                methodRouter(
                        performance.listReviews(),
                        performance.createReview()));

        // ========================================================
        // TRAINING
        // ========================================================

        server.createContext(
                "/api/training",
                methodRouter(
                        training.listTraining(),
                        training.createTraining()));

        server.createContext(
                "/api/training/",
                suffixRouter(
                        Map.of(
                                "/assign",
                                training.assignTraining(),

                                "/attendees",
                                training.attendees())));

        server.createContext(
                "/api/certifications",
                methodRouter(
                        training.listCertifications(),
                        training.createCertification()));

        // ========================================================
        // PROMOTIONS / TRANSFERS
        // ========================================================

        server.createContext(
                "/api/promotions",
                methodRouter(
                        career.listPromotions(),
                        career.createPromotion()));

        server.createContext(
                "/api/transfers",
                methodRouter(
                        career.listTransfers(),
                        career.createTransfer()));

        // ========================================================
        // ADMIN
        // ========================================================

        server.createContext(
                "/api/users",
                methodRouter(
                        admin.listUsers(),
                        admin.createUser()));

        server.createContext(
                "/api/users/",
                suffixRouter(
                        Map.of(
                                "/status",
                                admin.setAccountStatus(),

                                "/role",
                                admin.changeRole())));

        server.createContext(
                "/api/permissions",
                methodRouter(
                        admin.listPermissions(),
                        admin.createPermission()));

        server.createContext(
                "/api/permissions/role/",
                admin.permissionsForRole());

        server.createContext(
                "/api/role-permissions",
                methodRouter(
                        null,
                        admin.assignPermission()));

        // ========================================================
        // START
        // ========================================================

        server.start();

        System.out.println(
                "Employee Management System backend running on http://localhost:"
                        + port);
    }

    // ============================================================
    // METHOD ROUTER
    // ============================================================

    private static HttpHandler methodRouter(
            HttpHandler getHandler,
            HttpHandler postHandler) {

        return exchange -> {

            String method = exchange.getRequestMethod();

            // ----------------------------------------------------
            // OPTIONS
            // ----------------------------------------------------

            if ("OPTIONS".equalsIgnoreCase(method)) {

                HttpUtil.handlePreflight(exchange);

                return;
            }

            // ----------------------------------------------------
            // GET
            // ----------------------------------------------------

            if ("GET".equalsIgnoreCase(method)
                    && getHandler != null) {

                getHandler.handle(exchange);

                return;
            }

            // ----------------------------------------------------
            // POST / PUT
            // ----------------------------------------------------

            if (("POST".equalsIgnoreCase(method)
                    || "PUT".equalsIgnoreCase(method))
                    && postHandler != null) {

                postHandler.handle(exchange);

                return;
            }

            // ----------------------------------------------------
            // Unsupported method
            // ----------------------------------------------------

            HttpUtil.sendError(
                    exchange,
                    405,
                    "Method not allowed on this endpoint.");
        };
    }

    // ============================================================
    // EMPLOYEE ID ROUTER
    // ============================================================

    private static HttpHandler employeeByIdRouter(
            EmployeeController employees) {

        return exchange -> {

            String method = exchange.getRequestMethod();

            String path = exchange.getRequestURI()
                    .getPath();

            // ----------------------------------------------------
            // OPTIONS
            // ----------------------------------------------------

            if ("OPTIONS".equalsIgnoreCase(method)) {

                HttpUtil.handlePreflight(exchange);

                return;
            }

            // ----------------------------------------------------
            // DEACTIVATE
            // ----------------------------------------------------

            if (path.endsWith("/deactivate")) {

                employees
                        .deactivate()
                        .handle(exchange);

                return;
            }

            // ----------------------------------------------------
            // GET
            // ----------------------------------------------------

            if ("GET".equalsIgnoreCase(method)) {

                employees
                        .getById()
                        .handle(exchange);

                return;
            }

            // ----------------------------------------------------
            // PUT
            // ----------------------------------------------------

            if ("PUT".equalsIgnoreCase(method)) {

                employees
                        .update()
                        .handle(exchange);

                return;
            }

            HttpUtil.sendError(
                    exchange,
                    405,
                    "Method not allowed on this endpoint.");
        };
    }

    // ============================================================
    // SUFFIX ROUTER
    // ============================================================

    private static HttpHandler suffixRouter(
            Map<String, HttpHandler> actionsBySuffix) {

        Map<String, HttpHandler> actions = new LinkedHashMap<>(
                actionsBySuffix);

        return exchange -> {

            // ----------------------------------------------------
            // OPTIONS
            // ----------------------------------------------------

            if ("OPTIONS".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                HttpUtil.handlePreflight(exchange);

                return;
            }

            // ----------------------------------------------------
            // PATH
            // ----------------------------------------------------

            String path = exchange.getRequestURI()
                    .getPath();

            // ----------------------------------------------------
            // FIND ACTION
            // ----------------------------------------------------

            for (Map.Entry<String, HttpHandler> entry : actions.entrySet()) {

                if (path.endsWith(
                        entry.getKey())) {

                    entry.getValue()
                            .handle(exchange);

                    return;
                }
            }

            // ----------------------------------------------------
            // UNKNOWN ACTION
            // ----------------------------------------------------

            HttpUtil.sendError(
                    exchange,
                    404,
                    "Unknown action for this endpoint.");
        };
    }
}