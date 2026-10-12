
package com.sliit.ems.dao;

import com.sliit.ems.DatabaseConnectionManager;
import com.sliit.ems.model.Employee;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    private static final String BASE_SELECT = """
        SELECT e.Employee_ID, e.Employee_Number, e.First_Name, e.Middle_Name, e.Last_Name,
               e.Date_of_Birth, e.Gender, e.Phone_Number, e.Street, e.City, e.Postal_Code,
               e.Department_ID, d.Department_Name, e.Designation_ID, g.Designation_Name,
               e.Employment_Type, e.Employment_Status, e.Employment_Start_Date, e.Work_Location,
               e.Supervisor_Employee_ID, e.Annual_Leave_Balance, e.Casual_Leave_Balance, e.Sick_Leave_Balance
        FROM EMPLOYEE e
        JOIN DEPARTMENT d ON d.Department_ID = e.Department_ID
        JOIN DESIGNATION g ON g.Designation_ID = e.Designation_ID
        """;

    public List<Employee> findAll(String search, Integer departmentId)
            throws SQLException {

        StringBuilder sql = new StringBuilder(BASE_SELECT)
                .append(" WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(
                " AND (e.Employee_Number LIKE ? OR e.First_Name LIKE ? OR e.Last_Name LIKE ?) "
            );

            String like = "%" + search + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (departmentId != null) {
            sql.append(" AND e.Department_ID = ? ");
            params.add(departmentId);
        }

        sql.append(" ORDER BY e.Employee_ID");

        List<Employee> results = new ArrayList<>();

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps =
                     conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(map(rs));
                }
            }
        }

        return results;
    }

    public Employee findById(int employeeId) throws SQLException {

        String sql = BASE_SELECT + " WHERE e.Employee_ID = ?";

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean employeeNumberExists(String employeeNumber)
            throws SQLException {

        String sql =
                "SELECT 1 FROM EMPLOYEE WHERE Employee_Number = ?";

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, employeeNumber);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(Employee emp) throws SQLException {

        String sql = """
            INSERT INTO EMPLOYEE
            (Employee_Number, First_Name, Middle_Name, Last_Name, Date_of_Birth, Gender, Phone_Number,
             Street, City, Postal_Code, Department_ID, Designation_ID, Employment_Type, Employment_Status,
             Employment_Start_Date, Work_Location, Supervisor_Employee_ID)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            bindForInsertOrUpdate(ps, emp);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException(
                "Insert did not return a generated Employee_ID");
    }

    public void update(Employee emp) throws SQLException {

        String sql = """
            UPDATE EMPLOYEE SET
                First_Name = ?, Middle_Name = ?, Last_Name = ?, Date_of_Birth = ?, Gender = ?,
                Phone_Number = ?, Street = ?, City = ?, Postal_Code = ?, Department_ID = ?,
                Designation_ID = ?, Employment_Type = ?, Employment_Status = ?,
                Employment_Start_Date = ?, Work_Location = ?, Supervisor_Employee_ID = ?
            WHERE Employee_ID = ?
            """;

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, emp.firstName);
            ps.setString(2, emp.middleName);
            ps.setString(3, emp.lastName);
            ps.setDate(4, Date.valueOf(emp.dateOfBirth));
            ps.setString(5, emp.gender);
            ps.setString(6, emp.phoneNumber);
            ps.setString(7, emp.street);
            ps.setString(8, emp.city);
            ps.setString(9, emp.postalCode);
            ps.setInt(10, emp.departmentId);
            ps.setInt(11, emp.designationId);
            ps.setString(12, emp.employmentType);
            ps.setString(13, emp.employmentStatus);
            ps.setDate(14, Date.valueOf(emp.employmentStartDate));
            ps.setString(15, emp.workLocation);

            if (emp.supervisorEmployeeId != null) {
                ps.setInt(16, emp.supervisorEmployeeId);
            } else {
                ps.setNull(16, Types.INTEGER);
            }

            ps.setInt(17, emp.employeeId);
            ps.executeUpdate();
        }
    }

    /**
     * Employees can update only their permitted personal fields.
     */
    public void updateOwnProfile(
            int employeeId,
            String phoneNumber,
            String street,
            String city,
            String postalCode) throws SQLException {

        String sql = """
            UPDATE EMPLOYEE
            SET Phone_Number = ?, Street = ?, City = ?, Postal_Code = ?
            WHERE Employee_ID = ?
            """;

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, phoneNumber);
            ps.setString(2, street);
            ps.setString(3, city);
            ps.setString(4, postalCode);
            ps.setInt(5, employeeId);

            ps.executeUpdate();
        }
    }

    public void deactivate(int employeeId) throws SQLException {

        String sql = """
            UPDATE EMPLOYEE
            SET Employment_Status = 'TERMINATED'
            WHERE Employee_ID = ?
            """;

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, employeeId);
            ps.executeUpdate();
        }
    }

    public int countActive() throws SQLException {

        String sql = """
            SELECT COUNT(*)
            FROM EMPLOYEE
            WHERE Employment_Status = 'ACTIVE'
            """;

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getInt(1);
        }
    }

    public int countAll() throws SQLException {

        String sql = "SELECT COUNT(*) FROM EMPLOYEE";

        try (Connection conn =
                     DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return rs.getInt(1);
        }
    }

    private void bindForInsertOrUpdate(
            PreparedStatement ps, Employee emp) throws SQLException {

        ps.setString(1, emp.employeeNumber);
        ps.setString(2, emp.firstName);
        ps.setString(3, emp.middleName);
        ps.setString(4, emp.lastName);
        ps.setDate(5, Date.valueOf(emp.dateOfBirth));
        ps.setString(6, emp.gender);
        ps.setString(7, emp.phoneNumber);
        ps.setString(8, emp.street);
        ps.setString(9, emp.city);
        ps.setString(10, emp.postalCode);
        ps.setInt(11, emp.departmentId);
        ps.setInt(12, emp.designationId);
        ps.setString(13, emp.employmentType);
        ps.setString(14, emp.employmentStatus);
        ps.setDate(15, Date.valueOf(emp.employmentStartDate));
        ps.setString(16, emp.workLocation);

        if (emp.supervisorEmployeeId != null) {
            ps.setInt(17, emp.supervisorEmployeeId);
        } else {
            ps.setNull(17, Types.INTEGER);
        }
    }

    private Employee map(ResultSet rs) throws SQLException {

        Employee e = new Employee();

        e.employeeId = rs.getInt("Employee_ID");
        e.employeeNumber = rs.getString("Employee_Number");
        e.firstName = rs.getString("First_Name");
        e.middleName = rs.getString("Middle_Name");
        e.lastName = rs.getString("Last_Name");

        Date dob = rs.getDate("Date_of_Birth");
        e.dateOfBirth = dob == null ? null : dob.toLocalDate();

        e.gender = rs.getString("Gender");
        e.phoneNumber = rs.getString("Phone_Number");
        e.street = rs.getString("Street");
        e.city = rs.getString("City");
        e.postalCode = rs.getString("Postal_Code");

        e.departmentId = rs.getInt("Department_ID");
        e.departmentName = rs.getString("Department_Name");
        e.designationId = rs.getInt("Designation_ID");
        e.designationName = rs.getString("Designation_Name");

        e.employmentType = rs.getString("Employment_Type");
        e.employmentStatus = rs.getString("Employment_Status");

        Date startDate = rs.getDate("Employment_Start_Date");
        e.employmentStartDate =
                startDate == null ? null : startDate.toLocalDate();

        e.workLocation = rs.getString("Work_Location");

        int supervisorId = rs.getInt("Supervisor_Employee_ID");
        e.supervisorEmployeeId =
                rs.wasNull() ? null : supervisorId;

        e.annualLeaveBalance = rs.getDouble("Annual_Leave_Balance");
        e.casualLeaveBalance = rs.getDouble("Casual_Leave_Balance");
        e.sickLeaveBalance = rs.getDouble("Sick_Leave_Balance");

        return e;
    }
}
