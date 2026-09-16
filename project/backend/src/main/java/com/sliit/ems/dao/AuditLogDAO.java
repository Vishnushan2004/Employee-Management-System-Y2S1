package com.sliit.ems.dao;

import com.sliit.ems.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AuditLogDAO {

    public void record(int userId, String actionType, String module, String details) {
        // Audit logging must never break the primary action if it fails - log and swallow.
        String sql = "INSERT INTO AUDIT_LOG (User_ID, Action_Type, Module, Action_Details) VALUES (?,?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, module);
            ps.setString(4, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> findRecent(int limit) throws SQLException {
        String sql = """
            SELECT TOP (?) a.Audit_Log_ID, a.User_ID, u.Username, a.Action_Type, a.Module, a.Action_Date, a.Action_Details
            FROM AUDIT_LOG a
            JOIN USER_ACCOUNT u ON u.User_ID = a.User_ID
            ORDER BY a.Action_Date DESC
            """;
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("auditLogId", rs.getInt("Audit_Log_ID"));
                    row.put("username", rs.getString("Username"));
                    row.put("actionType", rs.getString("Action_Type"));
                    row.put("module", rs.getString("Module"));
                    row.put("actionDate", rs.getTimestamp("Action_Date").toString());
                    row.put("actionDetails", rs.getString("Action_Details"));
                    results.add(row);
                }
            }
        }
        return results;
    }
}
