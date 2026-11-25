package com.tukue.classroombooking.infrastructure.database;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigTest {
    @Test
    void usesLocalPostgresDefaultsWhenNoVariablesAreSet() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(Map.of());

        assertEquals("jdbc:postgresql://localhost:5433/classroom_booking", config.jdbcUrl());
        assertEquals("classroom_app", config.username());
        assertEquals("classroom_dev", config.password());
    }

    @Test
    void buildsUrlAndCredentialsFromEnvironment() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(Map.of(
                "POSTGRES_HOST", "db.example.test",
                "POSTGRES_PORT", "5440",
                "POSTGRES_DB", "training_rooms",
                "POSTGRES_USER", "booking_service",
                "POSTGRES_PASSWORD", "test-password"));

        assertEquals("jdbc:postgresql://db.example.test:5440/training_rooms", config.jdbcUrl());
        assertEquals("booking_service", config.username());
        assertEquals("test-password", config.password());
    }

    @Test
    void allowsExplicitJdbcUrlOverride() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(Map.of(
                "JDBC_URL", "jdbc:postgresql://custom-host:6000/custom_db"));

        assertEquals("jdbc:postgresql://custom-host:6000/custom_db", config.jdbcUrl());
    }
}
