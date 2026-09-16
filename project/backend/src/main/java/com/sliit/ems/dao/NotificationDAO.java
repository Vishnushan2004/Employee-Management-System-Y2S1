package com.sliit.ems.dao;

import com.sliit.ems.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotificationDAO {

    public void create(int userId, String type, String message) {
        String sql = "INSERT INTO NOTIFICATION (User_ID, Notification_Type, Message) VALUES (?,?,?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> findByUser(int userId) throws SQLException {
        String sql = "SELECT Notification_ID, Notification_Type, Message, Created_Date, Status, Read_Date " +
                "FROM NOTIFICATION WHERE User_ID = ? ORDER BY Created_Date DESC";
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("notificationId", rs.getInt("Notification_ID"));
                    row.put("notificationType", rs.getString("Notification_Type"));
                    row.put("message", rs.getString("Message"));
                    row.put("createdDate", rs.getTimestamp("Created_Date").toString());
                    row.put("status", rs.getString("Status"));
                    Timestamp readDate = rs.getTimestamp("Read_Date");
                    row.put("readDate", readDate == null ? null : readDate.toString());
                    results.add(row);
                }
            }
        }
        return results;
    }

    public void markRead(int notificationId) throws SQLException {
        String sql = "UPDATE NOTIFICATION SET Status = 'READ', Read_Date = GETDATE() WHERE Notification_ID = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        }
    }
}
