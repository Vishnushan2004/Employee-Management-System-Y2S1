package com.sliit.ems.controller.hr;

import com.sliit.ems.controller.AuthController;

import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.dao.NotificationDAO;
import com.sliit.ems.dao.hr.RecruitmentDAO;
import com.sliit.ems.dao.it.UserAccountDAO;
import com.sliit.ems.util.FileStorage;
import com.sliit.ems.util.HttpUtil;
import com.sliit.ems.util.Json;
import com.sliit.ems.util.Security;
import com.sliit.ems.util.SessionManager;
import com.sliit.ems.util.Validation;
import com.sun.net.httpserver.HttpHandler;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

public class RecruitmentController {

    private final RecruitmentDAO recruitmentDAO = new RecruitmentDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserAccountDAO userAccountDAO = new UserAccountDAO();

    private static final String HR = "HR_MANAGER";
    private static final Set<String> VALID_APPLICATION_STATUSES =
        Set.of("SUBMITTED", "SHORTLISTED", "INTERVIEWED", "OFFERED", "REJECTED", "HIRED");

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
                String jobTitle = Json.getString(body, "jobTitle");
                String jobDescription = Json.getString(body, "jobDescription");
                String requiredQualifications = Json.getString(body, "requiredQualifications");
                String requiredSkills = Json.getString(body, "requiredSkills");
                String openingDateStr = Json.getString(body, "openingDate");
                String closingDateStr = Json.getString(body, "closingDate");

                Validation.requireNonBlank(jobTitle, "Job title");
                Validation.requireMaxLength(jobTitle, 100, "Job title");
                Validation.requireMaxLength(jobDescription, 1000, "Job description");
                Validation.requireMaxLength(requiredQualifications, 500, "Required qualifications");
                Validation.requireMaxLength(requiredSkills, 500, "Required skills");
                LocalDate openingDate = Validation.parseDate(openingDateStr, "Opening date");
                LocalDate closingDate = Validation.parseDate(closingDateStr, "Closing date");
                Validation.requireDateNotBefore(closingDate, openingDate, "Closing date", "the opening date");

                int id = recruitmentDAO.createVacancy(jobTitle, jobDescription, requiredQualifications, requiredSkills,
                    openingDateStr, closingDateStr);
                auditLogDAO.record(session.userId, "CREATE", "JOB_VACANCY", "Created vacancy ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("vacancyId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendError(exchange, 400, e.getMessage());
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
                String name = Json.getString(body, "name");
                String email = Json.getString(body, "email");
                String phone = Json.getString(body, "phoneNumber");
                String qualifications = Json.getString(body, "qualifications");
                String skills = Json.getString(body, "skills");
                String experience = Json.getString(body, "workExperience");

                Validation.requireNonBlank(name, "Name");
                Validation.requireMaxLength(name, 100, "Name");
                Validation.requireEmail(email, "Email");
                Validation.requireMaxLength(email, 100, "Email");
                Validation.requirePhone(phone, "Phone number");
                Validation.requireMaxLength(qualifications, 500, "Qualifications");
                Validation.requireMaxLength(skills, 500, "Skills");
                Validation.requireMaxLength(experience, 500, "Work experience");

                int id = recruitmentDAO.createApplicant(name, email, phone, qualifications, skills, experience);
                auditLogDAO.record(session.userId, "CREATE", "APPLICANT", "Created applicant ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("applicantId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendError(exchange, 400, e.getMessage());
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
                String coverLetter = Json.getString(body, "coverLetter");
                Double expectedSalary = body.get("expectedSalary") != null ? Json.getDouble(body, "expectedSalary") : null;
                Validation.requireMaxLength(coverLetter, 1000, "Cover letter");
                Validation.requireNonNegative(expectedSalary, "Expected salary");

                int id = recruitmentDAO.createApplication(
                    Json.getInt(body, "applicantId"), Json.getInt(body, "vacancyId"),
                    Json.getString(body, "cvResume"), coverLetter, expectedSalary);
                auditLogDAO.record(session.userId, "CREATE", "APPLICATION", "Created application ID " + id);
                HttpUtil.sendJson(exchange, 201, Map.of("applicationId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendError(exchange, 400, e.getMessage());
            } catch (Exception e) {
                HttpUtil.sendError(exchange, 400, "Invalid application data: " + e.getMessage());
            }
        };
    }

    /** PUT /api/applications/{id}/status - HR only. Also notifies the applicant of the decision (mirrors LeaveController.notifyStatusChange). */
    public HttpHandler updateApplicationStatus() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            SessionManager.Session session = Security.requireRole(exchange, HR);
            if (session == null) return;
            int id = extractIdBeforeSuffix(exchange, "/status");
            try {
                Map<String, Object> body = HttpUtil.readJsonBody(exchange);
                String status = Json.getString(body, "status");
                if (status == null || !VALID_APPLICATION_STATUSES.contains(status)) {
                    HttpUtil.sendError(exchange, 400, "Invalid application status.");
                    return;
                }
                recruitmentDAO.updateApplicationStatus(id, status);
                auditLogDAO.record(session.userId, "UPDATE", "APPLICATION", "Updated application ID " + id + " status");
                notifyApplicationStatusChange(id, status);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Application updated"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            }
        };
    }

    /** GET /api/applications/{id}/cv - HR-side CV retrieval. HR is authorized to view any application's CV. */
    public HttpHandler downloadCvForHr() {
        return exchange -> {
            if (HttpUtil.handlePreflight(exchange)) return;
            if (Security.requireRole(exchange, HR) == null) return;
            int id = extractIdBeforeSuffix(exchange, "/cv");
            try {
                String storedFilename = recruitmentDAO.findApplicationCv(id);
                if (storedFilename == null) {
                    HttpUtil.sendError(exchange, 404, "No CV has been uploaded for this application.");
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
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (java.io.IOException e) {
                HttpUtil.sendError(exchange, 500, "Could not read the CV file.");
            }
        };
    }

    private void notifyApplicationStatusChange(int applicationId, String status) {
        try {
            Map<String, Object> app = recruitmentDAO.findApplicationOwnerAndJob(applicationId);
            if (app == null) return;
            Integer applicantId = (Integer) app.get("applicantId");
            if (applicantId == null) return;
            Integer userId = userAccountDAO.findUserIdByApplicantId(applicantId);
            if (userId == null) return; // applicant has no portal login (e.g. created by HR directly) - nothing to notify
            notificationDAO.create(userId, "APPLICATION_" + status,
                "Your application for \"" + app.get("jobTitle") + "\" is now " + status + ".");
        } catch (SQLException e) {
            System.err.println("Failed to send application status notification: " + e.getMessage());
        }
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
                Integer applicationId = Json.getInt(body, "applicationId");
                String interviewDateStr = Json.getString(body, "interviewDate");
                String interviewTime = Json.getString(body, "interviewTime");
                String location = Json.getString(body, "location");

                if (applicationId == null) {
                    HttpUtil.sendError(exchange, 400, "applicationId is required.");
                    return;
                }
                LocalDate interviewDate = Validation.parseDate(interviewDateStr, "Interview date");
                Validation.requireNotInPast(interviewDate, "Interview date");
                Validation.requireNonBlank(interviewTime, "Interview time");
                Validation.requireMaxLength(location, 100, "Location");

                int id = recruitmentDAO.scheduleInterview(applicationId, interviewDateStr, interviewTime, location);
                auditLogDAO.record(session.userId, "CREATE", "INTERVIEW", "Scheduled interview ID " + id);
                notifyInterviewScheduled(applicationId, interviewDateStr, interviewTime, location);
                HttpUtil.sendJson(exchange, 201, Map.of("interviewId", id));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendError(exchange, 400, e.getMessage());
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
                String remarks = Json.getString(body, "remarks");
                Validation.requireMaxLength(remarks, 500, "Remarks");
                recruitmentDAO.recordInterviewResult(id, Json.getString(body, "result"), remarks);
                auditLogDAO.record(session.userId, "UPDATE", "INTERVIEW", "Recorded result for interview ID " + id);
                HttpUtil.sendJson(exchange, 200, Map.of("message", "Interview result recorded"));
            } catch (SQLException e) {
                AuthController.handleDbError(exchange, e);
            } catch (IllegalArgumentException e) {
                HttpUtil.sendError(exchange, 400, e.getMessage());
            }
        };
    }

    private void notifyInterviewScheduled(int applicationId, String date, String time, String location) {
        try {
            Map<String, Object> app = recruitmentDAO.findApplicationOwnerAndJob(applicationId);
            if (app == null) return;
            Integer applicantId = (Integer) app.get("applicantId");
            if (applicantId == null) return;
            Integer userId = userAccountDAO.findUserIdByApplicantId(applicantId);
            if (userId == null) return;
            String loc = (location == null || location.isBlank()) ? "" : (" at " + location);
            notificationDAO.create(userId, "INTERVIEW_SCHEDULED",
                "An interview for \"" + app.get("jobTitle") + "\" has been scheduled on " + date + " " + time + loc + ".");
        } catch (SQLException e) {
            System.err.println("Failed to send interview scheduled notification: " + e.getMessage());
        }
    }

    private int extractIdBeforeSuffix(com.sun.net.httpserver.HttpExchange exchange, String suffix) {
        String path = exchange.getRequestURI().getPath();
        String trimmed = path.endsWith(suffix) ? path.substring(0, path.length() - suffix.length()) : path;
        String[] parts = trimmed.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }
}
