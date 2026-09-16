package com.sliit.ems.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple JDBC connection provider for Microsoft SQL Server.
 * A new Connection is opened per request and closed via try-with-resources
 * in the DAO layer - simple and correct enough for a local student project
 * (no connection pool needed at this scale).
 */
public class Database {

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "Microsoft JDBC Driver not found on classpath. " +
                "Download mssql-jdbc-<version>.jre11.jar and add it to backend/lib, " +
                "see README.md for the exact command.", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DBConfig.getConnectionUrl(),
                DBConfig.getUser(),
                DBConfig.getPassword()
        );
    }
}
