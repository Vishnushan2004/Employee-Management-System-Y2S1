package com.sliit.ems.model;

public class UserAccount {
    public int userId;
    public int employeeId; // 0 means this account has no EMPLOYEE record (an outside/applicant user)
    public int applicantId; // 0 means this account has no APPLICANT record (a staff/employee user)
    public String username;
    public String passwordHash;
    public int roleId;
    public String roleName; // joined, read-only
    public String accountStatus;
    public String lastLogin;
}
