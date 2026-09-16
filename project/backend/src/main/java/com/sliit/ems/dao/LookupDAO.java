package com.sliit.ems.dao;

import com.sliit.ems.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Simple read-only lookups for populating dropdowns - Department and Designation. */
public class LookupDAO {

    public List<Map<String, Object>> findAllDepartments() throws SQLException {
        return findAll("SELECT Department_ID AS id, Department_Name AS name FROM DEPARTMENT ORDER BY Department_Name");
    }

    public List<Map<String, Object>> findAllDesignations() throws SQLException {
        return findAll("SELECT Designation_ID AS id, Designation_Name AS name FROM DESIGNATION ORDER BY Designation_Name");
    }

    public List<Map<String, Object>> findAllRoles() throws SQLException {
        return findAll("SELECT Role_ID AS id, Role_Name AS name, Description AS description FROM ROLE ORDER BY Role_Name");
    }

    private List<Map<String, Object>> findAll(String sql) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }
        }
        return results;
    }
}
