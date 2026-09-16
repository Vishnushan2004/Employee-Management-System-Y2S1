package com.sliit.ems.model;

public class UserAccount {
    public int userId;
    public int employeeId;
    public String username;
    public String passwordHash;
    public int roleId;
    public String roleName; // joined, read-only
    public String accountStatus;
    public String lastLogin;
}
