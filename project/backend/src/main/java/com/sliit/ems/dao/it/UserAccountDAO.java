package com.sliit.ems.dao.it;

import com.sliit.ems.dao.QueryUtil;import com.sliit.ems.config.Database;
import com.sliit.ems.model.UserAccount;

import java.sql.*;

public class UserAccountDAO {

    /** Looks up an active account by username, joined with its role name. Returns null if not found. */
    public UserAccount findByUsername(String username) throws SQLException {
        String sql = """
            SELECT u.User_ID, u.Employee_ID, u.Applicant_ID, u.Username, u.Password_Hash, u.Role_ID,
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

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM USER_ACCOUNT WHERE Username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Creates a USER_ACCOUNT row for an outside/applicant user (Employee_ID left NULL). */
    public int createApplicantUserAccount(int applicantId, String username, String passwordHash, int roleId) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO USER_ACCOUNT (Applicant_ID, Username, Password_Hash, Role_ID, Account_Status) VALUES (?,?,?,?,'ACTIVE')",
            ps -> {
                ps.setInt(1, applicantId);
                ps.setString(2, username);
                ps.setString(3, passwordHash);
                ps.setInt(4, roleId);
            });
    }

    public void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE USER_ACCOUNT SET Last_Login = GETDATE() WHERE User_ID = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /** Looks up a Role_ID by its Role_Name (e.g. "OUTSIDE_USER"). Returns null if the role doesn't exist. */
    public Integer findRoleIdByName(String roleName) throws SQLException {
        String sql = "SELECT Role_ID FROM ROLE WHERE Role_Name = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
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

    public Integer findUserIdByApplicantId(int applicantId) throws SQLException {
        String sql = "SELECT User_ID FROM USER_ACCOUNT WHERE Applicant_ID = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private UserAccount map(ResultSet rs) throws SQLException {
        UserAccount u = new UserAccount();
        u.userId = rs.getInt("User_ID");
        u.employeeId = rs.getInt("Employee_ID"); // getInt() returns 0 for SQL NULL, used as "not applicable"
        u.applicantId = rs.getInt("Applicant_ID"); // same convention
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
