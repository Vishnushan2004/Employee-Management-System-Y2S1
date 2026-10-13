package com.sliit.ems.service;

import com.sliit.ems.dao.it.AuditLogDAO;
import com.sliit.ems.dao.it.UserAccountDAO;
import com.sliit.ems.model.UserAccount;
import com.sliit.ems.util.PasswordUtil;
import com.sliit.ems.util.SessionManager;

import java.sql.SQLException;

public class AuthService {

    private final UserAccountDAO userAccountDAO = new UserAccountDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    public static class LoginResult {
        public final boolean success;
        public final String message;
        public final String token;
        public final UserAccount account;

        private LoginResult(boolean success, String message, String token, UserAccount account) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.account = account;
        }

        static LoginResult fail(String message) { return new LoginResult(false, message, null, null); }
        static LoginResult ok(String token, UserAccount account) { return new LoginResult(true, null, token, account); }
    }

    public LoginResult login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return LoginResult.fail("Username and password are required.");
        }

        UserAccount account = userAccountDAO.findByUsername(username);
        if (account == null) {
            return LoginResult.fail("Invalid username or password.");
        }
        if (!"ACTIVE".equals(account.accountStatus)) {
            return LoginResult.fail("This account is " + account.accountStatus.toLowerCase() + ". Contact the IT Coordinator.");
        }
        if (!PasswordUtil.verify(password, account.passwordHash)) {
            return LoginResult.fail("Invalid username or password.");
        }

        String token = SessionManager.createSession(account.userId, account.employeeId, account.applicantId, account.username, account.roleName);
        userAccountDAO.updateLastLogin(account.userId);
        auditLogDAO.record(account.userId, "LOGIN", "AUTH", "User " + account.username + " logged in.");
        return LoginResult.ok(token, account);
    }

    public void logout(String token) {
        SessionManager.Session session = SessionManager.get(token);
        if (session != null) {
            auditLogDAO.record(session.userId, "LOGOUT", "AUTH", "User " + session.username + " logged out.");
        }
        SessionManager.invalidate(token);
    }
}
