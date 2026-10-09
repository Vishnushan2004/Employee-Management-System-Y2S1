package com.sliit.ems.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AdminDAO {

    /**
     * Lists every USER_ACCOUNT, staff or outside/applicant user. Employee_ID/Applicant_ID are now
     * mutually exclusive and nullable (see docs/database-design.md), so these are LEFT JOINs and
     * employeeName falls back to the applicant's name when the account belongs to an outside user.
     */
    public List<Map<String, Object>> findAllUsers() throws SQLException {
        return QueryUtil.query("""
            SELECT u.User_ID AS userId, u.Username AS username, u.Employee_ID AS employeeId, u.Applicant_ID AS applicantId,
                   COALESCE(e.First_Name + ' ' + e.Last_Name, a.Name) AS employeeName,
                   r.Role_ID AS roleId, r.Role_Name AS roleName,
                   u.Account_Status AS accountStatus, u.Last_Login AS lastLogin
            FROM USER_ACCOUNT u
            LEFT JOIN EMPLOYEE e ON e.Employee_ID = u.Employee_ID
            LEFT JOIN APPLICANT a ON a.Applicant_ID = u.Applicant_ID
            JOIN ROLE r ON r.Role_ID = u.Role_ID
            ORDER BY u.Username
            """, null);
    }

    public void setAccountStatus(int userId, String status) throws SQLException {
        QueryUtil.update("UPDATE USER_ACCOUNT SET Account_Status = ? WHERE User_ID = ?",
            ps -> { ps.setString(1, status); ps.setInt(2, userId); });
    }

    public void changeRole(int userId, int roleId) throws SQLException {
        QueryUtil.update("UPDATE USER_ACCOUNT SET Role_ID = ? WHERE User_ID = ?",
            ps -> { ps.setInt(1, roleId); ps.setInt(2, userId); });
    }

    /** Creates a login for an employee who doesn't have one yet. */
    public int createUserAccount(int employeeId, String username, String passwordHash, int roleId) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO USER_ACCOUNT (Employee_ID, Username, Password_Hash, Role_ID, Account_Status) VALUES (?,?,?,?,'ACTIVE')",
            ps -> {
                ps.setInt(1, employeeId);
                ps.setString(2, username);
                ps.setString(3, passwordHash);
                ps.setInt(4, roleId);
            });
    }

    // ---- Roles ----

    public int createRole(String name, String description) throws SQLException {
        return QueryUtil.insert("INSERT INTO ROLE (Role_Name, Description) VALUES (?,?)",
            ps -> { ps.setString(1, name); ps.setString(2, description); });
    }

    // ---- Permissions ----

    public List<Map<String, Object>> findAllPermissions() throws SQLException {
        return QueryUtil.query(
            "SELECT Permission_ID AS permissionId, Permission_Name AS permissionName, Module AS module, Action AS action FROM PERMISSION ORDER BY Module, Action",
            null);
    }

    public int createPermission(String name, String module, String action) throws SQLException {
        return QueryUtil.insert("INSERT INTO PERMISSION (Permission_Name, Module, Action) VALUES (?,?,?)",
            ps -> { ps.setString(1, name); ps.setString(2, module); ps.setString(3, action); });
    }

    public List<Map<String, Object>> findPermissionsForRole(int roleId) throws SQLException {
        return QueryUtil.query("""
            SELECT p.Permission_ID AS permissionId, p.Permission_Name AS permissionName, p.Module AS module, p.Action AS action
            FROM ROLE_PERMISSION rp JOIN PERMISSION p ON p.Permission_ID = rp.Permission_ID
            WHERE rp.Role_ID = ?
            """, ps -> ps.setInt(1, roleId));
    }

    public void assignPermission(int roleId, int permissionId) throws SQLException {
        // Idempotent: composite PK (Role_ID, Permission_ID) means a duplicate insert would fail,
        // so check first rather than relying on catching a PK violation.
        Map<String, Object> existing = QueryUtil.queryOne(
            "SELECT 1 AS x FROM ROLE_PERMISSION WHERE Role_ID = ? AND Permission_ID = ?",
            ps -> { ps.setInt(1, roleId); ps.setInt(2, permissionId); });
        if (existing != null) return;
        QueryUtil.update("INSERT INTO ROLE_PERMISSION (Role_ID, Permission_ID) VALUES (?,?)",
            ps -> { ps.setInt(1, roleId); ps.setInt(2, permissionId); });
    }

    public void removePermission(int roleId, int permissionId) throws SQLException {
        QueryUtil.update("DELETE FROM ROLE_PERMISSION WHERE Role_ID = ? AND Permission_ID = ?",
            ps -> { ps.setInt(1, roleId); ps.setInt(2, permissionId); });
    }
}
