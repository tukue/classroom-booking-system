package com.tukue.classroombooking.infrastructure.database;

import java.util.Map;

/** Holds PostgreSQL connection settings read from the process environment. */
public record DatabaseConfig(String jdbcUrl, String username, String password) {
    private static final String DEFAULT_DATABASE = "classroom_booking";
    private static final String DEFAULT_USERNAME = "classroom_app";
    private static final String DEFAULT_PASSWORD = "classroom_dev";

    /** Builds connection settings from environment variables, using local Compose defaults. */
    public static DatabaseConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static DatabaseConfig fromEnvironment(Map<String, String> environment) {
        String jdbcUrl = environment.getOrDefault("JDBC_URL", defaultJdbcUrl(environment));
        return new DatabaseConfig(
                jdbcUrl,
                environment.getOrDefault("POSTGRES_USER", DEFAULT_USERNAME),
                environment.getOrDefault("POSTGRES_PASSWORD", DEFAULT_PASSWORD));
    }

    private static String defaultJdbcUrl(Map<String, String> environment) {
        String host = environment.getOrDefault("POSTGRES_HOST", "localhost");
        String port = environment.getOrDefault("POSTGRES_PORT", "5433");
        String database = environment.getOrDefault("POSTGRES_DB", DEFAULT_DATABASE);
        return "jdbc:postgresql://%s:%s/%s".formatted(host, port, database);
    }
}
