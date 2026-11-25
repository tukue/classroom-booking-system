package com.classroombooking.infrastructure.database;

import java.util.Map;

/** Holds PostgreSQL connection settings read from the process environment. */
public record DatabaseConfig(String jdbcUrl, String username, String password) {
    private static final String DEFAULT_DATABASE = "classroom_booking";
    private static final String DEFAULT_USERNAME = "classroom_app";

    /** Builds connection settings from environment variables and requires an explicit password. */
    public static DatabaseConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static DatabaseConfig fromEnvironment(Map<String, String> environment) {
        String jdbcUrl = environment.getOrDefault("JDBC_URL", defaultJdbcUrl(environment));
        return new DatabaseConfig(
                jdbcUrl,
                environment.getOrDefault("POSTGRES_USER", DEFAULT_USERNAME),
                requiredPassword(environment));
    }

    private static String requiredPassword(Map<String, String> environment) {
        String password = environment.get("POSTGRES_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("POSTGRES_PASSWORD must be set in the environment or .env file.");
        }
        return password;
    }

    private static String defaultJdbcUrl(Map<String, String> environment) {
        String host = environment.getOrDefault("POSTGRES_HOST", "localhost");
        String port = environment.getOrDefault("POSTGRES_PORT", "5433");
        String database = environment.getOrDefault("POSTGRES_DB", DEFAULT_DATABASE);
        return "jdbc:postgresql://%s:%s/%s".formatted(host, port, database);
    }
}
