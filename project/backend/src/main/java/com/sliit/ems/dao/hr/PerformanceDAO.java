package com.sliit.ems.dao.hr;

import com.sliit.ems.dao.QueryUtil;import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class PerformanceDAO {

    public List<Map<String, Object>> findGoals(Integer employeeId) throws SQLException {
        String base = """
            SELECT g.Goal_ID AS goalId, g.Employee_ID AS employeeId, e.First_Name + ' ' + e.Last_Name AS employeeName,
                   g.Goal_Title AS goalTitle, g.Description AS description, g.Target_Value AS targetValue,
                   g.Progress AS progress, g.Status AS status
            FROM PERFORMANCE_GOAL g JOIN EMPLOYEE e ON e.Employee_ID = g.Employee_ID
            """;
        if (employeeId != null) {
            return QueryUtil.query(base + " WHERE g.Employee_ID = ? ORDER BY g.Goal_ID DESC", ps -> ps.setInt(1, employeeId));
        }
        return QueryUtil.query(base + " ORDER BY g.Goal_ID DESC", null);
    }

    public int createGoal(int employeeId, String title, String description, String targetValue) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO PERFORMANCE_GOAL (Employee_ID, Goal_Title, Description, Target_Value, Progress, Status) VALUES (?,?,?,?,0,'NOT_STARTED')",
            ps -> {
                ps.setInt(1, employeeId);
                ps.setString(2, title);
                ps.setString(3, description);
                ps.setString(4, targetValue);
            });
    }

    public void updateGoalProgress(int goalId, double progress, String status) throws SQLException {
        QueryUtil.update("UPDATE PERFORMANCE_GOAL SET Progress = ?, Status = ? WHERE Goal_ID = ?",
            ps -> { ps.setDouble(1, progress); ps.setString(2, status); ps.setInt(3, goalId); });
    }

    public List<Map<String, Object>> findReviews(Integer employeeId) throws SQLException {
        String base = """
            SELECT r.Review_ID AS reviewId, r.Employee_ID AS employeeId, e.First_Name + ' ' + e.Last_Name AS employeeName,
                   r.Review_Date AS reviewDate, r.Review_Period AS reviewPeriod,
                   r.Performance_Score AS performanceScore, r.Feedback AS feedback
            FROM PERFORMANCE_REVIEW r JOIN EMPLOYEE e ON e.Employee_ID = r.Employee_ID
            """;
        if (employeeId != null) {
            return QueryUtil.query(base + " WHERE r.Employee_ID = ? ORDER BY r.Review_Date DESC", ps -> ps.setInt(1, employeeId));
        }
        return QueryUtil.query(base + " ORDER BY r.Review_Date DESC", null);
    }

    public int createReview(int employeeId, String reviewDate, String reviewPeriod, Double score, String feedback) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO PERFORMANCE_REVIEW (Employee_ID, Review_Date, Review_Period, Performance_Score, Feedback) VALUES (?,?,?,?,?)",
            ps -> {
                ps.setInt(1, employeeId);
                ps.setDate(2, java.sql.Date.valueOf(reviewDate));
                ps.setString(3, reviewPeriod);
                if (score != null) ps.setDouble(4, score); else ps.setNull(4, java.sql.Types.DECIMAL);
                ps.setString(5, feedback);
            });
    }

    public int countPendingReviews() throws SQLException {
        // "Pending" here means an active employee with no review in the current calendar year - a simple, demonstrable definition.
        Map<String, Object> row = QueryUtil.queryOne("""
            SELECT COUNT(*) AS c FROM EMPLOYEE e
            WHERE e.Employment_Status = 'ACTIVE' AND NOT EXISTS (
                SELECT 1 FROM PERFORMANCE_REVIEW r WHERE r.Employee_ID = e.Employee_ID AND YEAR(r.Review_Date) = YEAR(GETDATE())
            )
            """, null);
        return row == null ? 0 : ((Number) row.get("c")).intValue();
    }
}
