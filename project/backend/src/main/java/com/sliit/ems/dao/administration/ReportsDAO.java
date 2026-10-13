package com.sliit.ems.dao.administration;

import com.sliit.ems.dao.QueryUtil;import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregate reports for the Administrative Officer's "Administrative
 * Reports" screen. These are summary/record-keeping reports only (headcount,
 * attendance, leave) - no payroll or recruitment-decision data, consistent
 * with the Administrative Officer's restricted permission set.
 */
public class ReportsDAO {

    /** Active headcount per department. */
    public List<Map<String, Object>> departmentHeadcount() throws SQLException {
        return QueryUtil.query("""
                SELECT d.Department_Name AS departmentName,
                       COUNT(e.Employee_ID) AS employeeCount
                FROM DEPARTMENT d
                LEFT JOIN EMPLOYEE e ON e.Department_ID = d.Department_ID AND e.Employment_Status = 'ACTIVE'
                GROUP BY d.Department_Name
                ORDER BY d.Department_Name
                """, null);
    }

    /** Attendance status breakdown for the current calendar month. */
    public List<Map<String, Object>> attendanceSummaryThisMonth() throws SQLException {
        return QueryUtil.query("""
                SELECT Attendance_Status AS attendanceStatus, COUNT(*) AS recordCount
                FROM ATTENDANCE
                WHERE YEAR(Attendance_Date) = YEAR(GETDATE()) AND MONTH(Attendance_Date) = MONTH(GETDATE())
                GROUP BY Attendance_Status
                ORDER BY Attendance_Status
                """, null);
    }

    /** Leave requests broken down by type and status, across all time. */
    public List<Map<String, Object>> leaveSummary() throws SQLException {
        return QueryUtil.query("""
                SELECT Leave_Type AS leaveType, Status AS status, COUNT(*) AS requestCount
                FROM LEAVE_REQUEST
                GROUP BY Leave_Type, Status
                ORDER BY Leave_Type, Status
                """, null);
    }

    /**
     * Simple administrative directory listing: name, department, designation,
     * status.
     */
    public List<Map<String, Object>> employeeDirectory() throws SQLException {
        return QueryUtil.query("""
                SELECT e.Employee_Number AS employeeNumber,
                       e.First_Name + ' ' + e.Last_Name AS employeeName,
                       d.Department_Name AS departmentName,
                       des.Designation_Name AS designationName,
                       e.Employment_Status AS employmentStatus
                FROM EMPLOYEE e
                JOIN DEPARTMENT d ON d.Department_ID = e.Department_ID
                JOIN DESIGNATION des ON des.Designation_ID = e.Designation_ID
                ORDER BY d.Department_Name, employeeName
                """, null);
    }
}