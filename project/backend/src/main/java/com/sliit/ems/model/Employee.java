package com.sliit.ems.model;

import java.time.LocalDate;

public class Employee {
    public int employeeId;
    public String employeeNumber;
    public String firstName;
    public String middleName;
    public String lastName;
    public LocalDate dateOfBirth;
    public String gender;
    public String phoneNumber;
    public String street;
    public String city;
    public String postalCode;
    public int departmentId;
    public String departmentName; // joined, read-only
    public int designationId;
    public String designationName; // joined, read-only
    public String employmentType;   // PERMANENT | CONTRACT
    public String employmentStatus;
    public LocalDate employmentStartDate;
    public String workLocation;
    public Integer supervisorEmployeeId;
    public double annualLeaveBalance;
    public double casualLeaveBalance;
    public double sickLeaveBalance;

    public String fullName() {
        StringBuilder sb = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) sb.append(' ').append(middleName);
        sb.append(' ').append(lastName);
        return sb.toString();
    }
}
