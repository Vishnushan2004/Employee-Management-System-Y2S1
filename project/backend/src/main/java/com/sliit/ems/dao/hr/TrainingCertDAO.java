package com.sliit.ems.dao.hr;

import com.sliit.ems.dao.QueryUtil;import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TrainingCertDAO {

    // ---- Training (M:N via EMPLOYEE_TRAINING) ----

    public List<Map<String, Object>> findAllTraining() throws SQLException {
        return QueryUtil.query("""
            SELECT Training_ID AS trainingId, Training_Title AS trainingTitle, Provider AS provider,
                   Start_Date AS startDate, End_Date AS endDate, Training_Type AS trainingType
            FROM TRAINING ORDER BY Start_Date DESC
            """, null);
    }

    public int createTraining(String title, String provider, String startDate, String endDate, String type) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO TRAINING (Training_Title, Provider, Start_Date, End_Date, Training_Type) VALUES (?,?,?,?,?)",
            ps -> {
                ps.setString(1, title);
                ps.setString(2, provider);
                ps.setDate(3, java.sql.Date.valueOf(startDate));
                if (endDate != null && !endDate.isBlank()) ps.setDate(4, java.sql.Date.valueOf(endDate));
                else ps.setNull(4, java.sql.Types.DATE);
                ps.setString(5, type);
            });
    }

    /** Assigns one or more employees to a training program (the M:N junction). */
    public void assignEmployees(int trainingId, List<Integer> employeeIds) throws SQLException {
        for (int employeeId : employeeIds) {
            QueryUtil.insert(
                "INSERT INTO EMPLOYEE_TRAINING (Employee_ID, Training_ID, Completion_Status) VALUES (?,?,'ASSIGNED')",
                ps -> { ps.setInt(1, employeeId); ps.setInt(2, trainingId); });
        }
    }

    public List<Map<String, Object>> findAttendeesForTraining(int trainingId) throws SQLException {
        return QueryUtil.query("""
            SELECT et.Employee_ID AS employeeId, e.First_Name + ' ' + e.Last_Name AS employeeName, et.Completion_Status AS completionStatus
            FROM EMPLOYEE_TRAINING et JOIN EMPLOYEE e ON e.Employee_ID = et.Employee_ID
            WHERE et.Training_ID = ?
            """, ps -> ps.setInt(1, trainingId));
    }

    public List<Map<String, Object>> findTrainingHistoryForEmployee(int employeeId) throws SQLException {
        return QueryUtil.query("""
            SELECT t.Training_ID AS trainingId, t.Training_Title AS trainingTitle, t.Provider AS provider,
                   t.Start_Date AS startDate, t.End_Date AS endDate, et.Completion_Status AS completionStatus
            FROM EMPLOYEE_TRAINING et JOIN TRAINING t ON t.Training_ID = et.Training_ID
            WHERE et.Employee_ID = ?
            ORDER BY t.Start_Date DESC
            """, ps -> ps.setInt(1, employeeId));
    }

    public void updateCompletionStatus(int employeeId, int trainingId, String status) throws SQLException {
        QueryUtil.update(
            "UPDATE EMPLOYEE_TRAINING SET Completion_Status = ? WHERE Employee_ID = ? AND Training_ID = ?",
            ps -> { ps.setString(1, status); ps.setInt(2, employeeId); ps.setInt(3, trainingId); });
    }

    // ---- Certification ----

    public List<Map<String, Object>> findAllCertifications() throws SQLException {
        return findCertifications(null);
    }

    public List<Map<String, Object>> findCertifications(Integer employeeId) throws SQLException {
        String base = """
            SELECT c.Certification_ID AS certificationId, c.Employee_ID AS employeeId,
                   e.First_Name + ' ' + e.Last_Name AS employeeName, c.Certification_Name AS certificationName,
                   c.Issuing_Organization AS issuingOrganization, c.Issue_Date AS issueDate,
                   c.Expiry_Date AS expiryDate, c.Status AS status
            FROM CERTIFICATION c JOIN EMPLOYEE e ON e.Employee_ID = c.Employee_ID
            """;
        if (employeeId != null) {
            return QueryUtil.query(base + " WHERE c.Employee_ID = ? ORDER BY c.Issue_Date DESC", ps -> ps.setInt(1, employeeId));
        }
        return QueryUtil.query(base + " ORDER BY c.Issue_Date DESC", null);
    }

    public int createCertification(int employeeId, String name, String issuer, String issueDate, String expiryDate) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO CERTIFICATION (Employee_ID, Certification_Name, Issuing_Organization, Issue_Date, Expiry_Date, Status) VALUES (?,?,?,?,?,'ACTIVE')",
            ps -> {
                ps.setInt(1, employeeId);
                ps.setString(2, name);
                ps.setString(3, issuer);
                ps.setDate(4, java.sql.Date.valueOf(issueDate));
                if (expiryDate != null && !expiryDate.isBlank()) ps.setDate(5, java.sql.Date.valueOf(expiryDate));
                else ps.setNull(5, java.sql.Types.DATE);
            });
    }

    public int countExpiringSoon() throws SQLException {
        Map<String, Object> row = QueryUtil.queryOne(
            "SELECT COUNT(*) AS c FROM CERTIFICATION WHERE Status = 'ACTIVE' AND Expiry_Date IS NOT NULL AND Expiry_Date <= DATEADD(DAY, 30, GETDATE())",
            null);
        return row == null ? 0 : ((Number) row.get("c")).intValue();
    }
}
