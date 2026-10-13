package com.sliit.ems.dao.hr;

import com.sliit.ems.dao.QueryUtil;import com.sliit.ems.config.Database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.List;
import java.util.Map;

public class RecruitmentDAO {

    // ---- Job Vacancies ----

    public List<Map<String, Object>> findVacancies() throws SQLException {
        return QueryUtil.query("""
            SELECT Vacancy_ID AS vacancyId, Job_Title AS jobTitle, Job_Description AS jobDescription,
                   Required_Qualifications AS requiredQualifications, Required_Skills AS requiredSkills,
                   Opening_Date AS openingDate, Closing_Date AS closingDate, Vacancy_Status AS vacancyStatus
            FROM JOB_VACANCY ORDER BY Opening_Date DESC
            """, null);
    }

    public int createVacancy(String title, String description, String qualifications, String skills,
                              String openingDate, String closingDate) throws SQLException {
        return QueryUtil.insert("""
            INSERT INTO JOB_VACANCY (Job_Title, Job_Description, Required_Qualifications, Required_Skills, Opening_Date, Closing_Date, Vacancy_Status)
            VALUES (?,?,?,?,?,?,'OPEN')
            """, ps -> {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setString(3, qualifications);
            ps.setString(4, skills);
            ps.setDate(5, Date.valueOf(openingDate));
            ps.setDate(6, Date.valueOf(closingDate));
        });
    }

    public void updateVacancyStatus(int vacancyId, String status) throws SQLException {
        QueryUtil.update("UPDATE JOB_VACANCY SET Vacancy_Status = ? WHERE Vacancy_ID = ?",
            ps -> { ps.setString(1, status); ps.setInt(2, vacancyId); });
    }

    // ---- Applicants ----

    public List<Map<String, Object>> findApplicants(String search) throws SQLException {
        String sql = "SELECT Applicant_ID AS applicantId, Name AS name, Email AS email, Phone_Number AS phoneNumber, " +
                     "Qualifications AS qualifications, Skills AS skills, Work_Experience AS workExperience FROM APPLICANT";
        if (search != null && !search.isBlank()) {
            return QueryUtil.query(sql + " WHERE Name LIKE ? OR Email LIKE ? ORDER BY Name",
                ps -> { String like = "%" + search + "%"; ps.setString(1, like); ps.setString(2, like); });
        }
        return QueryUtil.query(sql + " ORDER BY Name", null);
    }

    public int createApplicant(String name, String email, String phone, String qualifications, String skills, String experience) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO APPLICANT (Name, Email, Phone_Number, Qualifications, Skills, Work_Experience) VALUES (?,?,?,?,?,?)",
            ps -> {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setString(4, qualifications);
                ps.setString(5, skills);
                ps.setString(6, experience);
            });
    }

    // ---- Applications ----

    public List<Map<String, Object>> findApplications() throws SQLException {
        return QueryUtil.query("""
            SELECT a.Application_ID AS applicationId, a.Applicant_ID AS applicantId, ap.Name AS applicantName,
                   a.Vacancy_ID AS vacancyId, v.Job_Title AS jobTitle, a.Application_Date AS applicationDate,
                   a.Application_Status AS applicationStatus, a.CV_Resume AS cvResume, a.Cover_Letter AS coverLetter,
                   a.Expected_Salary AS expectedSalary
            FROM APPLICATION a
            JOIN APPLICANT ap ON ap.Applicant_ID = a.Applicant_ID
            JOIN JOB_VACANCY v ON v.Vacancy_ID = a.Vacancy_ID
            ORDER BY a.Application_Date DESC
            """, null);
    }

    public int createApplication(int applicantId, int vacancyId, String cvResume, String coverLetter, Double expectedSalary) throws SQLException {
        return QueryUtil.insert("""
            INSERT INTO APPLICATION (Applicant_ID, Vacancy_ID, CV_Resume, Cover_Letter, Expected_Salary, Application_Status)
            VALUES (?,?,?,?,?,'SUBMITTED')
            """, ps -> {
            ps.setInt(1, applicantId);
            ps.setInt(2, vacancyId);
            ps.setString(3, cvResume);
            ps.setString(4, coverLetter);
            if (expectedSalary != null) ps.setDouble(5, expectedSalary); else ps.setNull(5, java.sql.Types.DECIMAL);
        });
    }

    public void updateApplicationStatus(int applicationId, String status) throws SQLException {
        QueryUtil.update("UPDATE APPLICATION SET Application_Status = ? WHERE Application_ID = ?",
            ps -> { ps.setString(1, status); ps.setInt(2, applicationId); });
    }

    // ---- Interviews ----

    public List<Map<String, Object>> findInterviews() throws SQLException {
        return QueryUtil.query("""
            SELECT i.Interview_ID AS interviewId, i.Application_ID AS applicationId,
                   ap.Name AS applicantName, v.Job_Title AS jobTitle,
                   i.Interview_Date AS interviewDate, i.Interview_Time AS interviewTime,
                   i.Location AS location, i.Result AS result, i.Remarks AS remarks
            FROM INTERVIEW i
            JOIN APPLICATION a ON a.Application_ID = i.Application_ID
            JOIN APPLICANT ap ON ap.Applicant_ID = a.Applicant_ID
            JOIN JOB_VACANCY v ON v.Vacancy_ID = a.Vacancy_ID
            ORDER BY i.Interview_Date DESC
            """, null);
    }

    public int scheduleInterview(int applicationId, String date, String time, String location) throws SQLException {
        int id = QueryUtil.insert(
            "INSERT INTO INTERVIEW (Application_ID, Interview_Date, Interview_Time, Location, Result) VALUES (?,?,?,?,'PENDING')",
            ps -> {
                ps.setInt(1, applicationId);
                ps.setDate(2, Date.valueOf(date));
                ps.setTime(3, Time.valueOf(time + ":00"));
                ps.setString(4, location);
            });
        updateApplicationStatus(applicationId, "INTERVIEWED");
        return id;
    }

    public void recordInterviewResult(int interviewId, String result, String remarks) throws SQLException {
        QueryUtil.update("UPDATE INTERVIEW SET Result = ?, Remarks = ? WHERE Interview_ID = ?",
            ps -> { ps.setString(1, result); ps.setString(2, remarks); ps.setInt(3, interviewId); });
    }

    public int countOpenVacancies() throws SQLException {
        Map<String, Object> row = QueryUtil.queryOne("SELECT COUNT(*) AS c FROM JOB_VACANCY WHERE Vacancy_Status = 'OPEN'", null);
        return row == null ? 0 : ((Number) row.get("c")).intValue();
    }

    public int countPendingApplications() throws SQLException {
        Map<String, Object> row = QueryUtil.queryOne(
            "SELECT COUNT(*) AS c FROM APPLICATION WHERE Application_Status IN ('SUBMITTED','SHORTLISTED')", null);
        return row == null ? 0 : ((Number) row.get("c")).intValue();
    }

    // ================================================================
    // Outside-user (job applicant) portal support
    // ================================================================

    public boolean applicantEmailExists(String email) throws SQLException {
        Map<String, Object> row = QueryUtil.queryOne("SELECT 1 AS x FROM APPLICANT WHERE Email = ?",
            ps -> ps.setString(1, email));
        return row != null;
    }

    public static class RegisteredApplicant {
        public final int applicantId;
        public final int userId;
        public RegisteredApplicant(int applicantId, int userId) { this.applicantId = applicantId; this.userId = userId; }
    }

    /**
     * Creates the APPLICANT row and its USER_ACCOUNT login in a single transaction
     * (either both are created or neither is - no orphan APPLICANT without a login,
     * no orphan USER_ACCOUNT without an APPLICANT, matching the CK_UserAccount_ExactlyOneOwner
     * constraint). The account's Username is the applicant's email address.
     */
    public RegisteredApplicant registerApplicantUser(String name, String email, String phone, String passwordHash, int roleId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int applicantId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO APPLICANT (Name, Email, Phone_Number) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, phone);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Applicant insert did not return an ID");
                        applicantId = keys.getInt(1);
                    }
                }
                int userId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO USER_ACCOUNT (Applicant_ID, Username, Password_Hash, Role_ID, Account_Status) VALUES (?,?,?,?,'ACTIVE')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, applicantId);
                    ps.setString(2, email);
                    ps.setString(3, passwordHash);
                    ps.setInt(4, roleId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("User account insert did not return an ID");
                        userId = keys.getInt(1);
                    }
                }
                conn.commit();
                return new RegisteredApplicant(applicantId, userId);
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Map<String, Object> findApplicantProfile(int applicantId) throws SQLException {
        return QueryUtil.queryOne("""
            SELECT Applicant_ID AS applicantId, Name AS name, Email AS email, Phone_Number AS phoneNumber,
                   Qualifications AS qualifications, Skills AS skills, Work_Experience AS workExperience
            FROM APPLICANT WHERE Applicant_ID = ?
            """, ps -> ps.setInt(1, applicantId));
    }

    /** Email is deliberately NOT updatable here - it is the login username, changing it is out of scope. */
    public void updateApplicantProfile(int applicantId, String name, String phone, String qualifications, String skills, String experience) throws SQLException {
        QueryUtil.update("""
            UPDATE APPLICANT SET Name = ?, Phone_Number = ?, Qualifications = ?, Skills = ?, Work_Experience = ?
            WHERE Applicant_ID = ?
            """, ps -> {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, qualifications);
            ps.setString(4, skills);
            ps.setString(5, experience);
            ps.setInt(6, applicantId);
        });
    }

    public List<Map<String, Object>> findOpenVacancies() throws SQLException {
        return QueryUtil.query("""
            SELECT Vacancy_ID AS vacancyId, Job_Title AS jobTitle, Job_Description AS jobDescription,
                   Required_Qualifications AS requiredQualifications, Required_Skills AS requiredSkills,
                   Opening_Date AS openingDate, Closing_Date AS closingDate, Vacancy_Status AS vacancyStatus
            FROM JOB_VACANCY WHERE Vacancy_Status = 'OPEN' ORDER BY Opening_Date DESC
            """, null);
    }

    public boolean vacancyIsOpen(int vacancyId) throws SQLException {
        Map<String, Object> row = QueryUtil.queryOne("SELECT 1 AS x FROM JOB_VACANCY WHERE Vacancy_ID = ? AND Vacancy_Status = 'OPEN'",
            ps -> ps.setInt(1, vacancyId));
        return row != null;
    }

    public boolean applicantHasAppliedToVacancy(int applicantId, int vacancyId) throws SQLException {
        Map<String, Object> row = QueryUtil.queryOne(
            "SELECT 1 AS x FROM APPLICATION WHERE Applicant_ID = ? AND Vacancy_ID = ?",
            ps -> { ps.setInt(1, applicantId); ps.setInt(2, vacancyId); });
        return row != null;
    }

    public List<Map<String, Object>> findApplicationsByApplicant(int applicantId) throws SQLException {
        return QueryUtil.query("""
            SELECT a.Application_ID AS applicationId, a.Vacancy_ID AS vacancyId, v.Job_Title AS jobTitle,
                   a.Application_Date AS applicationDate, a.Application_Status AS applicationStatus,
                   a.CV_Resume AS cvResume, a.Cover_Letter AS coverLetter, a.Expected_Salary AS expectedSalary
            FROM APPLICATION a
            JOIN JOB_VACANCY v ON v.Vacancy_ID = a.Vacancy_ID
            WHERE a.Applicant_ID = ?
            ORDER BY a.Application_Date DESC
            """, ps -> ps.setInt(1, applicantId));
    }

    /**
     * Anti-IDOR ownership check: confirms Application_ID {applicationId} actually belongs to
     * Applicant_ID {applicantId}. MUST be called before returning or modifying any application-scoped
     * resource (the application itself, its CV, or its interview) on an outside-user-facing endpoint -
     * never trust a client-supplied application ID alone.
     */
    public boolean applicationBelongsToApplicant(int applicationId, int applicantId) throws SQLException {
        Map<String, Object> row = QueryUtil.queryOne(
            "SELECT 1 AS x FROM APPLICATION WHERE Application_ID = ? AND Applicant_ID = ?",
            ps -> { ps.setInt(1, applicationId); ps.setInt(2, applicantId); });
        return row != null;
    }

    public void updateApplicationCv(int applicationId, String storedFilename) throws SQLException {
        QueryUtil.update("UPDATE APPLICATION SET CV_Resume = ? WHERE Application_ID = ?",
            ps -> { ps.setString(1, storedFilename); ps.setInt(2, applicationId); });
    }

    /** Returns the stored CV filename for an application, or null if none uploaded yet. Caller must have already checked ownership/authorization. */
    public String findApplicationCv(int applicationId) throws SQLException {
        Map<String, Object> row = QueryUtil.queryOne("SELECT CV_Resume AS cvResume FROM APPLICATION WHERE Application_ID = ?",
            ps -> ps.setInt(1, applicationId));
        return row == null ? null : (String) row.get("cvResume");
    }

    /** Small lookup for notification purposes: who owns this application, and for which job. */
    public Map<String, Object> findApplicationOwnerAndJob(int applicationId) throws SQLException {
        return QueryUtil.queryOne("""
            SELECT a.Applicant_ID AS applicantId, v.Job_Title AS jobTitle
            FROM APPLICATION a JOIN JOB_VACANCY v ON v.Vacancy_ID = a.Vacancy_ID
            WHERE a.Application_ID = ?
            """, ps -> ps.setInt(1, applicationId));
    }

    public Map<String, Object> findInterviewByApplicationId(int applicationId) throws SQLException {
        return QueryUtil.queryOne("""
            SELECT Interview_ID AS interviewId, Application_ID AS applicationId, Interview_Date AS interviewDate,
                   Interview_Time AS interviewTime, Location AS location, Result AS result, Remarks AS remarks
            FROM INTERVIEW WHERE Application_ID = ?
            """, ps -> ps.setInt(1, applicationId));
    }
}
