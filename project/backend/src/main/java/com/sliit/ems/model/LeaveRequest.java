package com.sliit.ems.model;

public class LeaveRequest {
    public int leaveId;
    public int employeeId;
    public String employeeName; // joined, read-only
    public String leaveType;    // ANNUAL | CASUAL | SICK | OTHER
    public String startDate;
    public String endDate;
    public String reason;
    public String status;       // PENDING | APPROVED | REJECTED | CANCELLED
    public String appliedDate;
    public Integer approvedBy;
    public String approvedDate;
}
