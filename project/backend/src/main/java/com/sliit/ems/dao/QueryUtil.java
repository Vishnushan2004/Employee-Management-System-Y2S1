package com.sliit.ems.dao;

import com.sliit.ems.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small shared helper so the remaining modules (Shift, Payroll, Recruitment,
 * Performance, Training, Certification, Promotion, Transfer, Role/Permission)
 * don't each repeat the same ResultSet-to-Map boilerplate. Every query still
 * goes through PreparedStatement - this only removes repetitive mapping code,
 * not SQL safety.
 */
public class QueryUtil {

    public interface ParamBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    public static List<Map<String, Object>> query(String sql, ParamBinder binder) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (binder != null) binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        Object value = rs.getObject(i);
                        // Normalize SQL date/time types to strings so the JSON writer doesn't need to know about them
                        if (value instanceof Date || value instanceof Timestamp || value instanceof Time) {
                            value = value.toString();
                        }
                        row.put(meta.getColumnLabel(i), value);
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    public static Map<String, Object> queryOne(String sql, ParamBinder binder) throws SQLException {
        List<Map<String, Object>> rows = query(sql, binder);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** Runs an INSERT and returns the generated identity key. */
    public static int insert(String sql, ParamBinder binder) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (binder != null) binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Insert did not return a generated key");
    }

    public static int update(String sql, ParamBinder binder) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (binder != null) binder.bind(ps);
            return ps.executeUpdate();
        }
    }
}
