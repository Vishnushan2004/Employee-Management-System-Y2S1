package com.sliit.ems.dao.operations;

import com.sliit.ems.dao.QueryUtil;import java.sql.Date;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

public class ShiftDAO {

    public List<Map<String, Object>> findAllShifts() throws SQLException {
        return QueryUtil.query("""
            SELECT Shift_ID AS shiftId, Shift_Name AS shiftName, Shift_Type AS shiftType,
                   CONVERT(varchar, Start_Time, 108) AS startTime, CONVERT(varchar, End_Time, 108) AS endTime,
                   Working_Hours AS workingHours
            FROM SHIFT ORDER BY Shift_Name
            """, null);
    }

    public int createShift(String name, String type, String startTime, String endTime, double workingHours) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO SHIFT (Shift_Name, Shift_Type, Start_Time, End_Time, Working_Hours) VALUES (?,?,?,?,?)",
            ps -> {
                ps.setString(1, name);
                ps.setString(2, type);
                ps.setString(3, startTime);
                ps.setString(4, endTime);
                ps.setDouble(5, workingHours);
            });
    }

    public void updateShift(int shiftId, String name, String type, String startTime, String endTime, double workingHours) throws SQLException {
        QueryUtil.update(
            "UPDATE SHIFT SET Shift_Name=?, Shift_Type=?, Start_Time=?, End_Time=?, Working_Hours=? WHERE Shift_ID=?",
            ps -> {
                ps.setString(1, name);
                ps.setString(2, type);
                ps.setString(3, startTime);
                ps.setString(4, endTime);
                ps.setDouble(5, workingHours);
                ps.setInt(6, shiftId);
            });
    }

    public List<Map<String, Object>> findAllAssignments() throws SQLException {
        return QueryUtil.query("""
            SELECT sa.Assignment_ID AS assignmentId, sa.Employee_ID AS employeeId,
                   e.First_Name + ' ' + e.Last_Name AS employeeName,
                   sa.Shift_ID AS shiftId, s.Shift_Name AS shiftName,
                   sa.Assignment_Start_Date AS assignmentStartDate, sa.Assignment_End_Date AS assignmentEndDate
            FROM SHIFT_ASSIGNMENT sa
            JOIN EMPLOYEE e ON e.Employee_ID = sa.Employee_ID
            JOIN SHIFT s ON s.Shift_ID = sa.Shift_ID
            ORDER BY sa.Assignment_Start_Date DESC
            """, null);
    }

    public List<Map<String, Object>> findAssignmentsByEmployee(int employeeId) throws SQLException {
        return QueryUtil.query("""
            SELECT sa.Assignment_ID AS assignmentId, sa.Shift_ID AS shiftId, s.Shift_Name AS shiftName,
                   s.Start_Time AS startTime, s.End_Time AS endTime,
                   sa.Assignment_Start_Date AS assignmentStartDate, sa.Assignment_End_Date AS assignmentEndDate
            FROM SHIFT_ASSIGNMENT sa
            JOIN SHIFT s ON s.Shift_ID = sa.Shift_ID
            WHERE sa.Employee_ID = ?
            ORDER BY sa.Assignment_Start_Date DESC
            """, ps -> ps.setInt(1, employeeId));
    }

    public int assignShift(int employeeId, int shiftId, String startDate, String endDate) throws SQLException {
        return QueryUtil.insert(
            "INSERT INTO SHIFT_ASSIGNMENT (Employee_ID, Shift_ID, Assignment_Start_Date, Assignment_End_Date) VALUES (?,?,?,?)",
            ps -> {
                ps.setInt(1, employeeId);
                ps.setInt(2, shiftId);
                ps.setDate(3, Date.valueOf(startDate));
                if (endDate != null && !endDate.isBlank()) ps.setDate(4, Date.valueOf(endDate));
                else ps.setNull(4, Types.DATE);
            });
    }
}
