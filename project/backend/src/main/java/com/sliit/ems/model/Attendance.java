package com.sliit.ems.model;

public class Attendance {
    public int attendanceId;
    public int employeeId;
    public String employeeName; // joined, read-only
    public String attendanceDate;
    public String checkInTime;
    public String checkOutTime;
    public int lateMinutes;
    public double overtimeHours;
    public Double workingHours;
    public String attendanceStatus;
}
