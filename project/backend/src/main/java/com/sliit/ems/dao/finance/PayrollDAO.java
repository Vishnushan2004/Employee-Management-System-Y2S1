package com.sliit.ems.dao.finance;

import com.sliit.ems.config.Database;
import com.sliit.ems.dao.QueryUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PayrollDAO {

    /**
     * Retrieves payroll records, including employee numbers,
     * for payroll tables and finance manager reports.
     */
    public List<Map<String, Object>> findAll(
            Integer employeeId,
            Integer month,
            Integer year,
            String status
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    p.Payroll_ID AS payrollId,
                    p.Employee_ID AS employeeId,
                    e.Employee_Number AS employeeNumber,
                    e.First_Name + ' ' + e.Last_Name AS employeeName,
                    e.Department_ID AS departmentId,
                    p.Payroll_Month AS payrollMonth,
                    p.Payroll_Year AS payrollYear,
                    p.Basic_Salary AS basicSalary,
                    p.Gross_Salary AS grossSalary,
                    p.Net_Salary AS netSalary,
                    p.Payment_Date AS paymentDate,
                    p.Payroll_Status AS payrollStatus
                FROM PAYROLL p
                INNER JOIN EMPLOYEE e
                    ON e.Employee_ID = p.Employee_ID
                WHERE 1 = 1
                """);

        List<Object> params = new ArrayList<>();

        if (employeeId != null) {
            sql.append(" AND p.Employee_ID = ?");
            params.add(employeeId);
        }

        if (month != null) {
            sql.append(" AND p.Payroll_Month = ?");
            params.add(month);
        }

        if (year != null) {
            sql.append(" AND p.Payroll_Year = ?");
            params.add(year);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND p.Payroll_Status = ?");
            params.add(status.trim());
        }

        sql.append("""
                ORDER BY
                    p.Payroll_Year DESC,
                    p.Payroll_Month DESC,
                    e.Employee_Number ASC
                """);

        return QueryUtil.query(sql.toString(), ps -> {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
        });
    }

    public List<Map<String, Object>> findByEmployee(
            int employeeId
    ) throws SQLException {
        return findAll(employeeId, null, null, null);
    }

    public List<Map<String, Object>> findAllowances(
            int payrollId
    ) throws SQLException {
        return QueryUtil.query("""
                SELECT
                    Allowance_ID AS allowanceId,
                    Allowance_Type AS allowanceType,
                    Amount AS amount
                FROM ALLOWANCE
                WHERE Payroll_ID = ?
                """, ps -> ps.setInt(1, payrollId));
    }

    public List<Map<String, Object>> findDeductions(
            int payrollId
    ) throws SQLException {
        return QueryUtil.query("""
                SELECT
                    Deduction_ID AS deductionId,
                    Deduction_Type AS deductionType,
                    Amount AS amount
                FROM DEDUCTION
                WHERE Payroll_ID = ?
                """, ps -> ps.setInt(1, payrollId));
    }

    public Map<String, Object> findPayslip(
            int payrollId
    ) throws SQLException {
        return QueryUtil.queryOne("""
                SELECT
                    Payslip_ID AS payslipId,
                    Payroll_ID AS payrollId,
                    Pay_Date AS payDate,
                    Payment_Status AS paymentStatus
                FROM PAYSLIP
                WHERE Payroll_ID = ?
                """, ps -> ps.setInt(1, payrollId));
    }

    /**
     * Processes payroll within a database transaction.
     *
     * Gross salary = basic salary + allowances + overtime pay.
     * Net salary = gross salary - deductions.
     */
    public int processPayroll(
            int employeeId,
            int month,
            int year,
            double basicSalary,
            List<Map<String, Object>> allowances,
            List<Map<String, Object>> deductions,
            double overtimeRatePerHour
    ) throws SQLException {

        if (month < 1 || month > 12) {
            throw new SQLException("Payroll month must be between 1 and 12.");
        }

        if (year < 2000 || year > 2100) {
            throw new SQLException("Invalid payroll year.");
        }

        if (!Double.isFinite(basicSalary)
                || !Double.isFinite(overtimeRatePerHour)
                || basicSalary < 0
                || overtimeRatePerHour < 0) {
            throw new SQLException(
                    "Salary amounts must be finite and non-negative.");
        }

        if (allowances == null || deductions == null) {
            throw new SQLException(
                    "Allowances and deductions cannot be null.");
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try {
                double overtimeHours =
                        totalOvertimeHours(conn, employeeId, month, year);
                double overtimePay = overtimeHours * overtimeRatePerHour;

                // Use loops because getAmount() can throw SQLException.
                double allowanceTotal = 0;
                for (Map<String, Object> allowance : allowances) {
                    allowanceTotal += getAmount(allowance, "amount");
                }

                double deductionTotal = 0;
                for (Map<String, Object> deduction : deductions) {
                    deductionTotal += getAmount(deduction, "amount");
                }

                double gross = basicSalary + allowanceTotal + overtimePay;
                double net = gross - deductionTotal;

                if (!Double.isFinite(gross) || !Double.isFinite(net)) {
                    throw new SQLException("Calculated salary is not a valid number.");
                }

                if (net < 0) {
                    throw new SQLException(
                            "Deductions cannot exceed gross salary.");
                }

                int payrollId;

                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO PAYROLL (
                            Employee_ID,
                            Payroll_Month,
                            Payroll_Year,
                            Basic_Salary,
                            Gross_Salary,
                            Net_Salary,
                            Payroll_Status
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 'DRAFT')
                        """, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, employeeId);
                    ps.setInt(2, month);
                    ps.setInt(3, year);
                    ps.setDouble(4, basicSalary);
                    ps.setDouble(5, gross);
                    ps.setDouble(6, net);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Payroll insert did not return an ID.");
                        }
                        payrollId = keys.getInt(1);
                    }
                }

                if (overtimePay > 0) {
                    insertAllowance(conn, payrollId, "Overtime", overtimePay);
                }

                for (Map<String, Object> allowance : allowances) {
                    insertAllowance(
                            conn,
                            payrollId,
                            getType(allowance, "type"),
                            getAmount(allowance, "amount"));
                }

                for (Map<String, Object> deduction : deductions) {
                    try (PreparedStatement ps = conn.prepareStatement("""
                            INSERT INTO DEDUCTION (
                                Payroll_ID,
                                Deduction_Type,
                                Amount
                            )
                            VALUES (?, ?, ?)
                            """)) {
                        ps.setInt(1, payrollId);
                        ps.setString(2, getType(deduction, "type"));
                        ps.setDouble(3, getAmount(deduction, "amount"));
                        ps.executeUpdate();
                    }
                }

                /*
                 * FIX: Pay_Date is NOT NULL in this database.
                 * Supply a date when creating the pending payslip instead
                 * of inserting NULL. markPaid() will update it to the
                 * actual payment date when payroll is marked as paid.
                 */
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO PAYSLIP (
                            Payroll_ID,
                            Pay_Date,
                            Payment_Status
                        )
                        VALUES (?, CAST(GETDATE() AS DATE), 'PENDING')
                        """)) {
                    ps.setInt(1, payrollId);
                    ps.executeUpdate();
                }

                conn.commit();
                return payrollId;

            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }

                throw new SQLException("Unable to process payroll.", e);

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Validates and returns a numeric amount from a request map.
     */
    private double getAmount(
            Map<String, Object> item,
            String key
    ) throws SQLException {

        if (item == null) {
            throw new SQLException("Payroll item cannot be null.");
        }

        Object value = item.get(key);

        if (!(value instanceof Number number)) {
            throw new SQLException("Invalid or missing amount for " + key + ".");
        }

        double amount = number.doubleValue();

        if (!Double.isFinite(amount) || amount < 0) {
            throw new SQLException(
                    "Amount must be finite and non-negative.");
        }

        return amount;
    }

    private String getType(
            Map<String, Object> item,
            String key
    ) throws SQLException {

        if (item == null
                || item.get(key) == null
                || String.valueOf(item.get(key)).isBlank()) {
            throw new SQLException("Missing payroll item type: " + key);
        }

        return String.valueOf(item.get(key)).trim();
    }

    private void insertAllowance(
            Connection conn,
            int payrollId,
            String type,
            double amount
    ) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO ALLOWANCE (
                    Payroll_ID,
                    Allowance_Type,
                    Amount
                )
                VALUES (?, ?, ?)
                """)) {
            ps.setInt(1, payrollId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        }
    }

    private double totalOvertimeHours(
            Connection conn,
            int employeeId,
            int month,
            int year
    ) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT COALESCE(SUM(Overtime_Hours), 0)
                FROM ATTENDANCE
                WHERE Employee_ID = ?
                  AND MONTH(Attendance_Date) = ?
                  AND YEAR(Attendance_Date) = ?
                """)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getDouble(1);
            }
        }
    }

    /**
     * Changes DRAFT payroll to VERIFIED.
     */
    public void verify(int payrollId) throws SQLException {
        int updated = QueryUtil.update("""
                UPDATE PAYROLL
                SET Payroll_Status = 'VERIFIED'
                WHERE Payroll_ID = ?
                  AND Payroll_Status = 'DRAFT'
                """, ps -> ps.setInt(1, payrollId));

        if (updated == 0) {
            throw new SQLException(
                    "Payroll was not found or is not in DRAFT status.");
        }
    }

    /**
     * Marks verified payroll and its payslip as PAID in one transaction.
     */
    public void markPaid(int payrollId) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int updated;

                try (PreparedStatement ps = conn.prepareStatement("""
                        UPDATE PAYROLL
                        SET Payroll_Status = 'PAID',
                            Payment_Date = CAST(GETDATE() AS DATE)
                        WHERE Payroll_ID = ?
                          AND Payroll_Status = 'VERIFIED'
                        """)) {
                    ps.setInt(1, payrollId);
                    updated = ps.executeUpdate();
                }

                if (updated == 0) {
                    throw new SQLException(
                            "Payroll was not found or is not VERIFIED.");
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                        UPDATE PAYSLIP
                        SET Payment_Status = 'PAID',
                            Pay_Date = CAST(GETDATE() AS DATE)
                        WHERE Payroll_ID = ?
                        """)) {
                    ps.setInt(1, payrollId);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException(
                                "No payslip exists for this payroll record.");
                    }
                }

                conn.commit();

            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }

                throw new SQLException("Unable to mark payroll as paid.", e);

            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Returns payroll count and gross/net salary totals.
     */
    public Map<String, Object> summary(
            Integer month,
            Integer year
    ) throws SQLException {

        return QueryUtil.queryOne("""
                SELECT
                    COUNT(*) AS payrollCount,
                    COALESCE(SUM(Gross_Salary), 0) AS totalGross,
                    COALESCE(SUM(Net_Salary), 0) AS totalNet
                FROM PAYROLL
                WHERE (? IS NULL OR Payroll_Month = ?)
                  AND (? IS NULL OR Payroll_Year = ?)
                """, ps -> {
            if (month != null) {
                ps.setInt(1, month);
                ps.setInt(2, month);
            } else {
                ps.setNull(1, Types.INTEGER);
                ps.setNull(2, Types.INTEGER);
            }

            if (year != null) {
                ps.setInt(3, year);
                ps.setInt(4, year);
            } else {
                ps.setNull(3, Types.INTEGER);
                ps.setNull(4, Types.INTEGER);
            }
        });
    }
}
