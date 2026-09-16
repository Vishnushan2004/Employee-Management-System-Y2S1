package com.sliit.ems.dao;

import com.sliit.ems.config.Database;
import com.sliit.ems.model.LeaveRequest;

import java.sql.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LeaveRequestDAO {

    private static final String BASE_SELECT = """
        SELECT l.Leave_ID, l.Employee_ID, e.First_Name, e.Last_Name, l.Leave_Type,
               l.Start_Date, l.End_Date, l.Reason, l.Status, l.Applied_Date,
               l.Approved_By, l.Approved_Date
        FROM LEAVE_REQUEST l
        JOIN EMPLOYEE e ON e.Employee_ID = l.Employee_ID
        """;

    public List<LeaveRequest> findByEmployee(int employeeId) throws SQLException {
        String sql = BASE_SELECT + " WHERE l.Employee_ID = ? ORDER BY l.Applied_Date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return runQuery(ps);
        }
    }

    public List<LeaveRequest> findPending() throws SQLException {
        String sql = BASE_SELECT + " WHERE l.Status = 'PENDING' ORDER BY l.Applied_Date";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return runQuery(ps);
        }
    }

    public List<LeaveRequest> findAll() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY l.Applied_Date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return runQuery(ps);
        }
    }

    public LeaveRequest findById(int leaveId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            return findById(conn, leaveId);
        }
    }

    public LeaveRequest findById(Connection conn, int leaveId) throws SQLException {
        String sql = BASE_SELECT + " WHERE l.Leave_ID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int apply(int employeeId, String leaveType, String startDate, String endDate, String reason) throws SQLException {
        String sql = """
            INSERT INTO LEAVE_REQUEST (Employee_ID, Leave_Type, Start_Date, End_Date, Reason, Status)
            VALUES (?, ?, ?, ?, ?, 'PENDING')
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, employeeId);
            ps.setString(2, leaveType);
            ps.setDate(3, Date.valueOf(startDate));
            ps.setDate(4, Date.valueOf(endDate));
            ps.setString(5, reason);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert did not return a generated Leave_ID");
    }

    /**
     * Approves a PENDING leave request and deducts the corresponding balance
     * column on EMPLOYEE, in a single transaction so the two writes cannot
     * diverge if one fails.
     */
    public void approve(int leaveId, int approverEmployeeId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LeaveRequest leave = findById(conn, leaveId);
                if (leave == null) throw new SQLException("Leave request not found: " + leaveId);
                if (!"PENDING".equals(leave.status)) throw new SQLException("Only PENDING requests can be approved");

                long days = ChronoUnit.DAYS.between(
                        java.time.LocalDate.parse(leave.startDate),
                        java.time.LocalDate.parse(leave.endDate)) + 1;

                String balanceColumn = balanceColumnFor(leave.leaveType);
                if (balanceColumn != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE EMPLOYEE SET " + balanceColumn + " = " + balanceColumn + " - ? WHERE Employee_ID = ?")) {
                        ps.setLong(1, days);
                        ps.setInt(2, leave.employeeId);
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE LEAVE_REQUEST SET Status = 'APPROVED', Approved_By = ?, Approved_Date = GETDATE() WHERE Leave_ID = ?")) {
                    ps.setInt(1, approverEmployeeId);
                    ps.setInt(2, leaveId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void reject(int leaveId, int approverEmployeeId) throws SQLException {
        // Rejected leave never touches the balance, per the business rule.
        String sql = "UPDATE LEAVE_REQUEST SET Status = 'REJECTED', Approved_By = ?, Approved_Date = GETDATE() WHERE Leave_ID = ? AND Status = 'PENDING'";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, approverEmployeeId);
            ps.setInt(2, leaveId);
            ps.executeUpdate();
        }
    }

    /** Cancels a request. If it was previously APPROVED, restores the balance that was deducted. */
    public void cancel(int leaveId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LeaveRequest leave = findById(conn, leaveId);
                if (leave == null) throw new SQLException("Leave request not found: " + leaveId);
                if ("CANCELLED".equals(leave.status) || "REJECTED".equals(leave.status)) {
                    throw new SQLException("Leave request is already " + leave.status);
                }

                if ("APPROVED".equals(leave.status)) {
                    long days = ChronoUnit.DAYS.between(
                            java.time.LocalDate.parse(leave.startDate),
                            java.time.LocalDate.parse(leave.endDate)) + 1;
                    String balanceColumn = balanceColumnFor(leave.leaveType);
                    if (balanceColumn != null) {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "UPDATE EMPLOYEE SET " + balanceColumn + " = " + balanceColumn + " + ? WHERE Employee_ID = ?")) {
                            ps.setLong(1, days);
                            ps.setInt(2, leave.employeeId);
                            ps.executeUpdate();
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE LEAVE_REQUEST SET Status = 'CANCELLED' WHERE Leave_ID = ?")) {
                    ps.setInt(1, leaveId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public int countPending() throws SQLException {
        String sql = "SELECT COUNT(*) FROM LEAVE_REQUEST WHERE Status = 'PENDING'";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // Column names are fixed, internal constants (never user input), so string-building the
    // UPDATE statement here is safe - all *values* are still bound via PreparedStatement parameters.
    private static String balanceColumnFor(String leaveType) {
        return switch (leaveType) {
            case "ANNUAL" -> "Annual_Leave_Balance";
            case "CASUAL" -> "Casual_Leave_Balance";
            case "SICK" -> "Sick_Leave_Balance";
            default -> null; // OTHER leave type does not draw from a tracked balance
        };
    }

    private List<LeaveRequest> runQuery(PreparedStatement ps) throws SQLException {
        List<LeaveRequest> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) results.add(map(rs));
        }
        return results;
    }

    private LeaveRequest map(ResultSet rs) throws SQLException {
        LeaveRequest l = new LeaveRequest();
        l.leaveId = rs.getInt("Leave_ID");
        l.employeeId = rs.getInt("Employee_ID");
        l.employeeName = rs.getString("First_Name") + " " + rs.getString("Last_Name");
        l.leaveType = rs.getString("Leave_Type");
        l.startDate = rs.getDate("Start_Date").toString();
        l.endDate = rs.getDate("End_Date").toString();
        l.reason = rs.getString("Reason");
        l.status = rs.getString("Status");
        Timestamp applied = rs.getTimestamp("Applied_Date");
        l.appliedDate = applied == null ? null : applied.toString();
        int approvedBy = rs.getInt("Approved_By");
        l.approvedBy = rs.wasNull() ? null : approvedBy;
        Timestamp approvedDate = rs.getTimestamp("Approved_Date");
        l.approvedDate = approvedDate == null ? null : approvedDate.toString();
        return l;
    }
}
