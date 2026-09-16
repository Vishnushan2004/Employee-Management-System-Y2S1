package com.sliit.ems.dao;

import com.sliit.ems.config.Database;
import com.sliit.ems.model.UserAccount;

import java.sql.*;

public class UserAccountDAO {

    /** Looks up an active account by username, joined with its role name. Returns null if not found. */
    public UserAccount findByUsername(String username) throws SQLException {
        String sql = """
            SELECT u.User_ID, u.Employee_ID, u.Username, u.Password_Hash, u.Role_ID,
                   r.Role_Name, u.Account_Status, u.Last_Login
            FROM USER_ACCOUNT u
            JOIN ROLE r ON r.Role_ID = u.Role_ID
            WHERE u.Username = ?
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE USER_ACCOUNT SET Last_Login = GETDATE() WHERE User_ID = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public Integer findUserIdByEmployeeId(int employeeId) throws SQLException {
        String sql = "SELECT User_ID FROM USER_ACCOUNT WHERE Employee_ID = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private UserAccount map(ResultSet rs) throws SQLException {
        UserAccount u = new UserAccount();
        u.userId = rs.getInt("User_ID");
        u.employeeId = rs.getInt("Employee_ID");
        u.username = rs.getString("Username");
        u.passwordHash = rs.getString("Password_Hash");
        u.roleId = rs.getInt("Role_ID");
        u.roleName = rs.getString("Role_Name");
        u.accountStatus = rs.getString("Account_Status");
        Timestamp lastLogin = rs.getTimestamp("Last_Login");
        u.lastLogin = lastLogin == null ? null : lastLogin.toString();
        return u;
    }
}
