package com.sliit.ems.controller;

import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.dao.hr.RecruitmentDAO;
import com.sliit.ems.util.FileStorage;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sliit.ems.util.Validation;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;

/**
 * Endpoints for the public job-applicant ("outside user") self-service portal.
 *
 * SECURITY (anti-IDOR): every handler here that takes an application ID first
 * confirms, server-side, that the authenticated applicant (session.applicantId)
 * actually owns that Application_ID via RecruitmentDAO.applicationBelongsToApplicant(...)
 * before reading or writing anything. A client-supplied ID is never trusted on
 * its own - see the project owner's standing instruction on IDOR prevention.
 */
public class ApplicantPortalController {

    private final RecruitmentDAO recruitmentDAO = new RecruitmentDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final String ROLE = "OUTSIDE_USER";

    // ---- Public (no login required) ----

    /** GET /api/public/vacancies - public listing of OPEN vacancies only, no auth required. */
    public HttpHandler publicOpenVacancies() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendError(exchange, 405, "Method not allowed");
                return;
            }
            try {
                HttpUtil.sendJson(exchange, 200, recruitmentDAO.findOpenVacancies());
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    // ---- Own profile ----

    public HttpHandler getProfile() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, ROLE);
            if (session == null) return;
            try {
                Map<String, Object> profile = recruitmentDAO.findApplicantProfile(session.applicantId);
                if (profile == null) {
                    HttpUtil.sendError(exchange, 404, "Applicant profile not found.");
                    return;
                }
                HttpUtil.sendJson(exchange, 200, profile);
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** PUT /api/applicant/profile - Name/Phone/Qualifications/Skills/Work Experience only (not Email). */
    public HttpHandler updateProfile() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, ROLE);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                String name = Json.getString(body, "name");
                String phone = Json.getString(body, "phoneNumber");
                String qualifications = Json.getString(body, "qualifications");
                String skills = Json.getString(body, "skills");
                String experience = Json.getString(body, "workExperience");

                Validation.requireNonBlank(name, "Name");
                Validation.requireMaxLength(name, 100, "Name");
                Validation.requirePhone(phone, "Phone number");
                Validation.requireMaxLength(qualifications, 500, "Qualifications");
                Validation.requireMaxLength(skills, 500, "Skills");
                Validation.requireMaxLength(experience, 500, "Work experience");

                recruitmentDAO.updateApplicantProfile(session.applicantId, name, phone, qualifications, skills, experience);
                auditLogDAO.record(session.userId, "UPDATE", "APPLICANT_SELF_SERVICE", "Applicant updated own profile");
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Profile updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendError(exchange, 400, e.getMessage());
            }
        };
    }

    // ---- Own applications ----

    /** GET /api/applicant/applications - own applications and their status. */
    public HttpHandler listMyApplications() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, ROLE);
            if (session == null) return;
            try {
                HttpUtil.sendJson(exchange, 200, recruitmentDAO.findApplicationsByApplicant(session.applicantId));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** POST /api/applicant/applications - apply to a vacancy. Applicant_ID always comes from the session, never the request body. */
    public HttpHandler apply() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, ROLE);
            if (session == null) return;
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                Integer vacancyId = Json.getInt(body, "vacancyId");
                if (vacancyId == null) {
                    HttpUtil.sendError(exchange, 400, "vacancyId is required.");
                    return;
                }
                String coverLetter = Json.getString(body, "coverLetter");
                Double expectedSalary = body.get("expectedSalary") != null ? Json.getDouble(body, "expectedSalary") : null;

                Validation.requireMaxLength(coverLetter, 1000, "Cover letter");
                Validation.requireNonNegative(expectedSalary, "Expected salary");

                if (!recruitmentDAO.vacancyIsOpen(vacancyId)) {
                    HttpUtil.sendError(exchange, 400, "This vacancy is not open for applications.");
                    return;
                }
                if (recruitmentDAO.applicantHasAppliedToVacancy(session.applicantId, vacancyId)) {
                    HttpUtil.sendError(exchange, 409, "You have already applied to this vacancy.");
                    return;
                }

                int id = recruitmentDAO.createApplication(session.applicantId, vacancyId, null, coverLetter, expectedSalary);
                auditLogDAO.record(session.userId, "CREATE", "APPLICATION", "Applicant applied to vacancy ID " + vacancyId);
                HttpUtil.sendJson(exchange, 201, Map.of("applicationId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendError(exchange, 400, e.getMessage());
            }
        };
    }

    /** GET /api/applicant/applications/{id}/interview - own interview details, ownership-checked. */
    public HttpHandler interviewForApplication() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, ROLE);
            if (session == null) return;
            int applicationId = extractIdBeforeSuffix(exchange, "/interview");
            try {
                if (!recruitmentDAO.applicationBelongsToApplicant(applicationId, session.applicantId)) {
                    HttpUtil.sendError(exchange, 403, "You do not have access to this application.");
                    return;
                }
                Map<String, Object> interview = recruitmentDAO.findInterviewByApplicationId(applicationId);
                if (interview == null) {
                    HttpUtil.sendError(exchange, 404, "No interview has been scheduled for this application yet.");
                    return;
                }
                HttpUtil.sendJson(exchange, 200, interview);
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    // ---- CV upload / download (own) ----

    /** GET (download) or POST (upload) /api/applicant/applications/{id}/cv - both ownership-checked. */
    public HttpHandler cv() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, ROLE);
            if (session == null) return;
            int applicationId = extractIdBeforeSuffix(exchange, "/cv");
            try {
                if (!recruitmentDAO.applicationBelongsToApplicant(applicationId, session.applicantId)) {
                    HttpUtil.sendError(exchange, 403, "You do not have access to this application.");
                    return;
                }
                String method = exchange.getRequestMethod();
                if ("POST".equalsIgnoreCase(method)) {
                    uploadCv(exchange, session, applicationId);
                } else if ("GET".equalsIgnoreCase(method)) {
                    downloadCv(exchange, applicationId);
                } else {
                    HttpUtil.sendError(exchange, 405, "Method not allowed on this endpoint.");
                }
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    private void uploadCv(HttpExchange exchange, SessionManager.Session session, int applicationId) throws IOException, SQLException {
        Map<String, Object> body = HttpUtil.readJsonBody(exchange);
        String filename = Json.getString(body, "filename");
        String fileContentBase64 = Json.getString(body, "fileContentBase64");
        try {
            String storedFilename = FileStorage.saveCv(filename, fileContentBase64);
            recruitmentDAO.updateApplicationCv(applicationId, storedFilename);
            auditLogDAO.record(session.userId, "UPDATE", "APPLICATION", "Uploaded CV for application ID " + applicationId);
            HttpUtil.sendJson(exchange, 200, Map.of("message", "CV uploaded", "cvResume", storedFilename));
        } catch (FileStorage.ValidationException e) {
            HttpUtil.sendError(exchange, 400, e.getMessage());
        }
    }

    private void downloadCv(HttpExchange exchange, int applicationId) throws IOException, SQLException {
        String storedFilename = recruitmentDAO.findApplicationCv(applicationId);
        if (storedFilename == null) {
            HttpUtil.sendError(exchange, 404, "No CV has been uploaded for this application yet.");
            return;
        }
        byte[] fileBytes = FileStorage.readCv(storedFilename);
        if (fileBytes == null) {
            HttpUtil.sendError(exchange, 404, "CV file could not be found on the server.");
            return;
        }
        HttpUtil.sendJson(exchange, 200, Map.of(
            "filename", storedFilename,
            "fileContentBase64", Base64.getEncoder().encodeToString(fileBytes)));
    }

    private int extractIdBeforeSuffix(HttpExchange exchange, String suffix) {
        String path = exchange.getRequestURI().getPath();
        String trimmed = path.endsWith(suffix) ? path.substring(0, path.length() - suffix.length()) : path;
        String[] parts = trimmed.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }
}
