package com.classroombooking;

import com.classroombooking.infrastructure.database.DatabaseInitializer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @Test
    void displaysDatabaseFailureAndReturnsFailureCode() {
        DatabaseInitializer initializer = new DatabaseInitializer(
                () -> { throw new SQLException("database unavailable"); });
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();

        int exitCode;
        try (PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
             PrintStream error = new PrintStream(errorBytes, true, StandardCharsets.UTF_8)) {
            exitCode = Main.run(initializer, output, error);
        }

        assertEquals(1, exitCode);
        assertEquals("", outputBytes.toString(StandardCharsets.UTF_8));
        assertTrue(errorBytes.toString(StandardCharsets.UTF_8)
                .contains("Unable to initialize the database"));
    }
}
