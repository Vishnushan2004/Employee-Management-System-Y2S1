package com.sliit.ems.dao;

import com.sliit.ems.config.Database;
import com.sliit.ems.model.Attendance;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    private static final String BASE_SELECT = """
        SELECT a.Attendance_ID, a.Employee_ID, e.First_Name, e.Last_Name, a.Attendance_Date,
               a.Check_In_Time, a.Check_Out_Time, a.Late_Minutes, a.Overtime_Hours,
               a.Working_Hours, a.Attendance_Status
        FROM ATTENDANCE a
        JOIN EMPLOYEE e ON e.Employee_ID = a.Employee_ID
        """;

    // Standard shift assumed to start at 09:00 for late-minute calculation when no shift lookup is wired up.
    private static final LocalTime STANDARD_START = LocalTime.of(9, 0);
    private static final double STANDARD_WORKING_HOURS = 8.0;

    public List<Attendance> findByEmployee(int employeeId) throws SQLException {
        String sql = BASE_SELECT + " WHERE a.Employee_ID = ? ORDER BY a.Attendance_Date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return runQuery(ps);
        }
    }

    public List<Attendance> findByDate(String date) throws SQLException {
        String sql = BASE_SELECT + " WHERE a.Attendance_Date = ? ORDER BY e.Employee_ID";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            return runQuery(ps);
        }
    }

    public List<Attendance> findAll() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY a.Attendance_Date DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return runQuery(ps);
        }
    }

    /** Returns the existing open (checked-in, not checked-out) row for today, or null. */
    public Attendance findOpenToday(int employeeId) throws SQLException {
        String sql = BASE_SELECT + " WHERE a.Employee_ID = ? AND a.Attendance_Date = CAST(GETDATE() AS DATE)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int checkIn(int employeeId) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        int lateMinutes = 0;
        if (now.toLocalTime().isAfter(STANDARD_START)) {
            lateMinutes = (int) Duration.between(STANDARD_START, now.toLocalTime()).toMinutes();
        }
        String status = lateMinutes > 0 ? "LATE" : "PRESENT";

        String sql = """
            INSERT INTO ATTENDANCE (Employee_ID, Attendance_Date, Check_In_Time, Late_Minutes, Attendance_Status)
            VALUES (?, CAST(GETDATE() AS DATE), GETDATE(), ?, ?)
            """;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, lateMinutes);
            ps.setString(3, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert did not return a generated Attendance_ID");
    }

    /** Calculates working hours and overtime from check-in/out timestamps rather than trusting client input. */
    public void checkOut(int attendanceId) throws SQLException {
        String selectSql = "SELECT Check_In_Time FROM ATTENDANCE WHERE Attendance_ID = ?";
        Timestamp checkIn;
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, attendanceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Attendance record not found: " + attendanceId);
                checkIn = rs.getTimestamp("Check_In_Time");
                if (checkIn == null) throw new SQLException("Cannot check out without a check-in time");
            }
        }

        LocalDateTime checkInTime = checkIn.toLocalDateTime();
        LocalDateTime checkOutTime = LocalDateTime.now();
        double workingHours = Duration.between(checkInTime, checkOutTime).toMinutes() / 60.0;
        double overtime = Math.max(0, workingHours - STANDARD_WORKING_HOURS);

        String sql = "UPDATE ATTENDANCE SET Check_Out_Time = ?, Working_Hours = ?, Overtime_Hours = ? WHERE Attendance_ID = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(checkOutTime));
            ps.setDouble(2, Math.round(workingHours * 100.0) / 100.0);
            ps.setDouble(3, Math.round(overtime * 100.0) / 100.0);
            ps.setInt(4, attendanceId);
            ps.executeUpdate();
        }
    }

    /** For authorized correction of attendance records (Operations/Admin). */
    public void correct(int attendanceId, String status, Integer lateMinutes) throws SQLException {
        String sql = "UPDATE ATTENDANCE SET Attendance_Status = ?, Late_Minutes = COALESCE(?, Late_Minutes) WHERE Attendance_ID = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (lateMinutes != null) ps.setInt(2, lateMinutes); else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, attendanceId);
            ps.executeUpdate();
        }
    }

    public int countPresentToday() throws SQLException {
        return countTodayByStatus("PRESENT", "LATE");
    }

    public int countAbsentToday() throws SQLException {
        return countTodayByStatus("ABSENT");
    }

    public int countLateToday() throws SQLException {
        return countTodayByStatus("LATE");
    }

    private int countTodayByStatus(String... statuses) throws SQLException {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }
        String sql = "SELECT COUNT(*) FROM ATTENDANCE WHERE Attendance_Date = CAST(GETDATE() AS DATE) AND Attendance_Status IN (" + placeholders + ")";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < statuses.length; i++) ps.setString(i + 1, statuses[i]);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<Attendance> runQuery(PreparedStatement ps) throws SQLException {
        List<Attendance> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) results.add(map(rs));
        }
        return results;
    }

    private Attendance map(ResultSet rs) throws SQLException {
        Attendance a = new Attendance();
        a.attendanceId = rs.getInt("Attendance_ID");
        a.employeeId = rs.getInt("Employee_ID");
        a.employeeName = rs.getString("First_Name") + " " + rs.getString("Last_Name");
        a.attendanceDate = rs.getDate("Attendance_Date").toString();
        Timestamp checkIn = rs.getTimestamp("Check_In_Time");
        a.checkInTime = checkIn == null ? null : checkIn.toString();
        Timestamp checkOut = rs.getTimestamp("Check_Out_Time");
        a.checkOutTime = checkOut == null ? null : checkOut.toString();
        a.lateMinutes = rs.getInt("Late_Minutes");
        a.overtimeHours = rs.getDouble("Overtime_Hours");
        double workingHours = rs.getDouble("Working_Hours");
        a.workingHours = rs.wasNull() ? null : workingHours;
        a.attendanceStatus = rs.getString("Attendance_Status");
        return a;
    }
}
