
package com.sliit.ems;

import java.sql.Connection;
import java.sql.SQLException;
import com.sliit.ems.config.Database;

/*Singleton manager for database connection access */

public final class DatabaseConnectionManager {

    private static final DatabaseConnectionManager INSTANCE =
            new DatabaseConnectionManager();

    private DatabaseConnectionManager() {
    }

    public static DatabaseConnectionManager getInstance() {
        return INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        return Database.getConnection();
    }
}
