package com.tukue.classroombooking.infrastructure.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Singleton JDBC connection factory; each call creates a separately managed connection. */
public final class DatabaseManager implements ConnectionFactory {
    private final DatabaseConfig config;

    private DatabaseManager() {
        this(DatabaseConfig.fromEnvironment());
    }

    DatabaseManager(DatabaseConfig config) {
        this.config = config;
    }

    /** Returns the single application-level connection factory instance. */
    public static DatabaseManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /** Opens a new connection; callers own and must close the returned connection. */
    @Override
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl(), config.username(), config.password());
    }

    private static final class InstanceHolder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }
}
