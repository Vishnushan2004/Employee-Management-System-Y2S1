package com.sliit.ems.dao;

import com.sliit.ems.config.Database;

import java.sql.*;
import java.util.List;
import java.util.Map;

public class PayrollDAO {

    public List<Map<String, Object>> findAll(Integer employeeId, Integer month, Integer year, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT p.Payroll_ID AS payrollId, p.Employee_ID AS employeeId,
                   e.First_Name + ' ' + e.Last_Name AS employeeName, e.Department_ID AS departmentId,
                   p.Payroll_Month AS payrollMonth, p.Payroll_Year AS payrollYear,
                   p.Basic_Salary AS basicSalary, p.Gross_Salary AS grossSalary, p.Net_Salary AS netSalary,
                   p.Payment_Date AS paymentDate, p.Payroll_Status AS payrollStatus
            FROM PAYROLL p JOIN EMPLOYEE e ON e.Employee_ID = p.Employee_ID
            WHERE 1=1
            """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (employeeId != null) { sql.append(" AND p.Employee_ID = ?"); params.add(employeeId); }
        if (month != null) { sql.append(" AND p.Payroll_Month = ?"); params.add(month); }
        if (year != null) { sql.append(" AND p.Payroll_Year = ?"); params.add(year); }
        if (status != null && !status.isBlank()) { sql.append(" AND p.Payroll_Status = ?"); params.add(status); }
        sql.append(" ORDER BY p.Payroll_Year DESC, p.Payroll_Month DESC");

        return QueryUtil.query(sql.toString(), ps -> {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
        });
    }

    public List<Map<String, Object>> findByEmployee(int employeeId) throws SQLException {
        return findAll(employeeId, null, null, null);
    }

    public List<Map<String, Object>> findAllowances(int payrollId) throws SQLException {
        return QueryUtil.query(
            "SELECT Allowance_ID AS allowanceId, Allowance_Type AS allowanceType, Amount AS amount FROM ALLOWANCE WHERE Payroll_ID = ?",
            ps -> ps.setInt(1, payrollId));
    }

    public List<Map<String, Object>> findDeductions(int payrollId) throws SQLException {
        return QueryUtil.query(
            "SELECT Deduction_ID AS deductionId, Deduction_Type AS deductionType, Amount AS amount FROM DEDUCTION WHERE Payroll_ID = ?",
            ps -> ps.setInt(1, payrollId));
    }

    public Map<String, Object> findPayslip(int payrollId) throws SQLException {
        return QueryUtil.queryOne(
            "SELECT Payslip_ID AS payslipId, Payroll_ID AS payrollId, Pay_Date AS payDate, Payment_Status AS paymentStatus FROM PAYSLIP WHERE Payroll_ID = ?",
            ps -> ps.setInt(1, payrollId));
    }

    /**
     * Processes payroll for one employee/month/year in a single transaction:
     * inserts PAYROLL with Gross/Net computed from basic salary + allowances - deductions
     * (+ overtime pay, looked up from ATTENDANCE for that month), then inserts the
     * ALLOWANCE/DEDUCTION rows and a PENDING PAYSLIP.
     */
    public int processPayroll(int employeeId, int month, int year, double basicSalary,
                               List<Map<String, Object>> allowances, List<Map<String, Object>> deductions,
                               double overtimeRatePerHour) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double overtimeHours = totalOvertimeHours(conn, employeeId, month, year);
                double overtimePay = overtimeHours * overtimeRatePerHour;

                double allowanceTotal = allowances.stream().mapToDouble(a -> ((Number) a.get("amount")).doubleValue()).sum();
                double deductionTotal = deductions.stream().mapToDouble(d -> ((Number) d.get("amount")).doubleValue()).sum();

                double gross = basicSalary + allowanceTotal + overtimePay;
                double net = gross - deductionTotal;

                int payrollId;
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO PAYROLL (Employee_ID, Payroll_Month, Payroll_Year, Basic_Salary, Gross_Salary, Net_Salary, Payroll_Status)
                        VALUES (?,?,?,?,?,?,'DRAFT')
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, employeeId);
                    ps.setInt(2, month);
                    ps.setInt(3, year);
                    ps.setDouble(4, basicSalary);
                    ps.setDouble(5, gross);
                    ps.setDouble(6, net);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Payroll insert did not return an ID");
                        payrollId = keys.getInt(1);
                    }
                }

                if (overtimePay > 0) {
                    insertAllowance(conn, payrollId, "Overtime", overtimePay);
                }
                for (Map<String, Object> a : allowances) {
                    insertAllowance(conn, payrollId, (String) a.get("type"), ((Number) a.get("amount")).doubleValue());
                }
                for (Map<String, Object> d : deductions) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO DEDUCTION (Payroll_ID, Deduction_Type, Amount) VALUES (?,?,?)")) {
                        ps.setInt(1, payrollId);
                        ps.setString(2, (String) d.get("type"));
                        ps.setDouble(3, ((Number) d.get("amount")).doubleValue());
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO PAYSLIP (Payroll_ID, Pay_Date, Payment_Status) VALUES (?, CAST(GETDATE() AS DATE), 'PENDING')")) {
                    ps.setInt(1, payrollId);
                    ps.executeUpdate();
                }

                conn.commit();
                return payrollId;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void insertAllowance(Connection conn, int payrollId, String type, double amount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ALLOWANCE (Payroll_ID, Allowance_Type, Amount) VALUES (?,?,?)")) {
            ps.setInt(1, payrollId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        }
    }

    private double totalOvertimeHours(Connection conn, int employeeId, int month, int year) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COALESCE(SUM(Overtime_Hours), 0) FROM ATTENDANCE
                WHERE Employee_ID = ? AND MONTH(Attendance_Date) = ? AND YEAR(Attendance_Date) = ?
                """)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    public void verify(int payrollId) throws SQLException {
        QueryUtil.update("UPDATE PAYROLL SET Payroll_Status = 'VERIFIED' WHERE Payroll_ID = ? AND Payroll_Status = 'DRAFT'",
            ps -> ps.setInt(1, payrollId));
    }

    /** Marks payroll as PAID and the linked payslip as PAID, in one transaction. */
    public void markPaid(int payrollId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE PAYROLL SET Payroll_Status = 'PAID', Payment_Date = CAST(GETDATE() AS DATE) WHERE Payroll_ID = ? AND Payroll_Status = 'VERIFIED'")) {
                    ps.setInt(1, payrollId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE PAYSLIP SET Payment_Status = 'PAID', Pay_Date = CAST(GETDATE() AS DATE) WHERE Payroll_ID = ?")) {
                    ps.setInt(1, payrollId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException se) throw se;
                throw new SQLException(e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Map<String, Object> summary(Integer month, Integer year) throws SQLException {
        return QueryUtil.queryOne("""
            SELECT COUNT(*) AS payrollCount, COALESCE(SUM(Gross_Salary),0) AS totalGross,
                   COALESCE(SUM(Net_Salary),0) AS totalNet
            FROM PAYROLL WHERE (? IS NULL OR Payroll_Month = ?) AND (? IS NULL OR Payroll_Year = ?)
            """, ps -> {
                if (month != null) ps.setInt(1, month); else ps.setNull(1, Types.INTEGER);
                if (month != null) ps.setInt(2, month); else ps.setNull(2, Types.INTEGER);
                if (year != null) ps.setInt(3, year); else ps.setNull(3, Types.INTEGER);
                if (year != null) ps.setInt(4, year); else ps.setNull(4, Types.INTEGER);
            });
    }
}
