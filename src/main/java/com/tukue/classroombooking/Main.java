package com.tukue.classroombooking;

import com.tukue.classroombooking.infrastructure.database.DatabaseInitializer;
import com.tukue.classroombooking.infrastructure.database.DatabaseManager;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Objects;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(new DatabaseInitializer(DatabaseManager.getInstance()),
                System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(DatabaseInitializer initializer, PrintStream output, PrintStream error) {
        Objects.requireNonNull(initializer);
        Objects.requireNonNull(output);
        Objects.requireNonNull(error);
        try {
            initializer.initialize();
            output.println("Classroom Booking System database is ready.");
            return 0;
        } catch (SQLException | IOException exception) {
            error.println("Unable to initialize the database. Start PostgreSQL with `docker compose up -d database`.");
            return 1;
        }
    }
}
