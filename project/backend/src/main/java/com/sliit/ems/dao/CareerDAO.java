package com.sliit.ems.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class CareerDAO {

    public List<Map<String, Object>> findPromotions(Integer employeeId) throws SQLException {
        String base = """
            SELECT p.Promotion_ID AS promotionId, p.Employee_ID AS employeeId, e.First_Name + ' ' + e.Last_Name AS employeeName,
                   p.Previous_Designation AS previousDesignation, p.New_Designation AS newDesignation,
                   p.Effective_Date AS effectiveDate, p.Reason AS reason
            FROM PROMOTION p JOIN EMPLOYEE e ON e.Employee_ID = p.Employee_ID
            """;
        if (employeeId != null) {
            return QueryUtil.query(base + " WHERE p.Employee_ID = ? ORDER BY p.Effective_Date DESC", ps -> ps.setInt(1, employeeId));
        }
        return QueryUtil.query(base + " ORDER BY p.Effective_Date DESC", null);
    }

    /** Records a promotion and updates the employee's current designation, in one transaction. */
    public int recordPromotion(int employeeId, String previousDesignation, int newDesignationId, String newDesignationName,
                                String effectiveDate, String reason) throws SQLException {
        try (var conn = com.sliit.ems.config.Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int promotionId;
                try (var ps = conn.prepareStatement(
                        "INSERT INTO PROMOTION (Employee_ID, Previous_Designation, New_Designation, Effective_Date, Reason) VALUES (?,?,?,?,?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, employeeId);
                    ps.setString(2, previousDesignation);
                    ps.setString(3, newDesignationName);
                    ps.setDate(4, java.sql.Date.valueOf(effectiveDate));
                    ps.setString(5, reason);
                    ps.executeUpdate();
                    try (var keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Promotion insert did not return an ID");
                        promotionId = keys.getInt(1);
                    }
                }
                try (var ps = conn.prepareStatement("UPDATE EMPLOYEE SET Designation_ID = ? WHERE Employee_ID = ?")) {
                    ps.setInt(1, newDesignationId);
                    ps.setInt(2, employeeId);
                    ps.executeUpdate();
                }
                conn.commit();
                return promotionId;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Map<String, Object>> findTransfers(Integer employeeId) throws SQLException {
        String base = """
            SELECT t.Transfer_ID AS transferId, t.Employee_ID AS employeeId, e.First_Name + ' ' + e.Last_Name AS employeeName,
                   t.Previous_Department AS previousDepartment, t.New_Department AS newDepartment,
                   t.Effective_Date AS effectiveDate, t.Reason AS reason
            FROM TRANSFER t JOIN EMPLOYEE e ON e.Employee_ID = t.Employee_ID
            """;
        if (employeeId != null) {
            return QueryUtil.query(base + " WHERE t.Employee_ID = ? ORDER BY t.Effective_Date DESC", ps -> ps.setInt(1, employeeId));
        }
        return QueryUtil.query(base + " ORDER BY t.Effective_Date DESC", null);
    }

    public int recordTransfer(int employeeId, String previousDepartment, int newDepartmentId, String newDepartmentName,
                               String effectiveDate, String reason) throws SQLException {
        try (var conn = com.sliit.ems.config.Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int transferId;
                try (var ps = conn.prepareStatement(
                        "INSERT INTO TRANSFER (Employee_ID, Previous_Department, New_Department, Effective_Date, Reason) VALUES (?,?,?,?,?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, employeeId);
                    ps.setString(2, previousDepartment);
                    ps.setString(3, newDepartmentName);
                    ps.setDate(4, java.sql.Date.valueOf(effectiveDate));
                    ps.setString(5, reason);
                    ps.executeUpdate();
                    try (var keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Transfer insert did not return an ID");
                        transferId = keys.getInt(1);
                    }
                }
                try (var ps = conn.prepareStatement("UPDATE EMPLOYEE SET Department_ID = ? WHERE Employee_ID = ?")) {
                    ps.setInt(1, newDepartmentId);
                    ps.setInt(2, employeeId);
                    ps.executeUpdate();
                }
                conn.commit();
                return transferId;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
