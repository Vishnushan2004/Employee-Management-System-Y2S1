package com.sliit.ems.controller;

import com.sliit.ems.dao.AuditLogDAO;
import com.sliit.ems.dao.RecruitmentDAO;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.util.Map;

public class RecruitmentController {

    private final RecruitmentDAO recruitmentDAO = new RecruitmentDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String HR = "HR_MANAGER";

    // ---- Vacancies ----

    public HttpHandler listVacancies() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (Security.requireAuth(exchange) == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, recruitmentDAO.findVacancies());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createVacancy() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = recruitmentDAO.createVacancy(
                    Json.getString(body, "jobTitle"), Json.getString(body, "jobDescription"),
                    Json.getString(body, "requiredQualifications"), Json.getString(body, "requiredSkills"),
                    Json.getString(body, "openingDate"), Json.getString(body, "closingDate"));
                auditLogDAO.record(session.userId, "CREATE", "JOB_VACANCY", "Created vacancy ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("vacancyId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid vacancy data: " + e.getMessage());
            }
        };
    }

    public HttpHandler updateVacancyStatus() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            int id = extractIdBeforeSuffix(exchange, "/status");
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                recruitmentDAO.updateVacancyStatus(id, Json.getString(body, "status"));
                auditLogDAO.record(session.userId, "UPDATE", "JOB_VACANCY", "Updated vacancy ID " + id + " status");
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Vacancy updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    // ---- Applicants ----

    public HttpHandler listApplicants() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (Security.requireRole(exchange, HR) == null) return;
            try {
                Map<String, String> query = HttpUtil.parseQuery(exchange);
                HttpUtil.sendJson(exchange, 200, recruitmentDAO.findApplicants(query.get("search")));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createApplicant() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = recruitmentDAO.createApplicant(
                    Json.getString(body, "name"), Json.getString(body, "email"), Json.getString(body, "phoneNumber"),
                    Json.getString(body, "qualifications"), Json.getString(body, "skills"), Json.getString(body, "workExperience"));
                auditLogDAO.record(session.userId, "CREATE", "APPLICANT", "Created applicant ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("applicantId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid applicant data: " + e.getMessage());
            }
        };
    }

    // ---- Applications ----

    public HttpHandler listApplications() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (Security.requireRole(exchange, HR) == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, recruitmentDAO.findApplications());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler createApplication() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                Double expectedSalary = body.get("expectedSalary") != null ? Json.getDouble(body, "expectedSalary") : null;
                int id = recruitmentDAO.createApplication(
                    Json.getInt(body, "applicantId"), Json.getInt(body, "vacancyId"),
                    Json.getString(body, "cvResume"), Json.getString(body, "coverLetter"), expectedSalary);
                auditLogDAO.record(session.userId, "CREATE", "APPLICATION", "Created application ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("applicationId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid application data: " + e.getMessage());
            }
        };
    }

    public HttpHandler updateApplicationStatus() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            int id = extractIdBeforeSuffix(exchange, "/status");
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                recruitmentDAO.updateApplicationStatus(id, Json.getString(body, "status"));
                auditLogDAO.record(session.userId, "UPDATE", "APPLICATION", "Updated application ID " + id + " status");
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Application updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    // ---- Interviews ----

    public HttpHandler listInterviews() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (Security.requireRole(exchange, HR) == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, recruitmentDAO.findInterviews());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    public HttpHandler scheduleInterview() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                int id = recruitmentDAO.scheduleInterview(
                    Json.getInt(body, "applicationId"), Json.getString(body, "interviewDate"),
                    Json.getString(body, "interviewTime"), Json.getString(body, "location"));
                auditLogDAO.record(session.userId, "CREATE", "INTERVIEW", "Scheduled interview ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("interviewId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid interview data: " + e.getMessage());
            }
        };
    }

    public HttpHandler recordInterviewResult() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            int id = extractIdBeforeSuffix(exchange, "/result");
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                recruitmentDAO.recordInterviewResult(id, Json.getString(body, "result"), Json.getString(body, "remarks"));
                auditLogDAO.record(session.userId, "UPDATE", "INTERVIEW", "Recorded result for interview ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Interview result recorded"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
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
