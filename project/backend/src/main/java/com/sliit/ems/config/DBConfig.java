package com.sliit.ems.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads local database + server configuration from config.properties.
 * config.properties is NOT committed to git (see .gitignore) - copy
 * config.properties.example to config.properties and fill in your own
 * local SQL Server details.
 */
public class DBConfig {

    private static final Properties props = new Properties();
    private static boolean loaded = false;

    private static synchronized void load() {
        if (loaded) return;
        Path configPath = Path.of("config.properties");
        if (Files.exists(configPath)) {
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read config.properties: " + e.getMessage(), e);
            }
        } else {
            System.err.println("WARNING: config.properties not found in working directory. " +
                    "Copy config.properties.example to config.properties and set your DB credentials.");
        }
        loaded = true;
    }

    public static String get(String key, String defaultValue) {
        load();
        return props.getProperty(key, defaultValue);
    }

    public static String getConnectionUrl() {
        load();
        String host = get("db.host", "localhost");
        String port = get("db.port", "1433");
        String name = get("db.name", "EmployeeManagementSystem");
        boolean trust = Boolean.parseBoolean(get("db.trustServerCertificate", "true"));
        return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + name
                + ";trustServerCertificate=" + trust;
    }

    public static String getUser() {
        return get("db.user", "sa");
    }

    public static String getPassword() {
        return get("db.password", "");
    }

    public static int getServerPort() {
        return Integer.parseInt(get("server.port", "8080"));
    }
}
