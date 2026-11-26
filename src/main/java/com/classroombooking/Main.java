package com.classroombooking;

import com.classroombooking.infrastructure.database.DatabaseInitializer;
import com.classroombooking.infrastructure.database.DatabaseManager;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(() -> new DatabaseInitializer(DatabaseManager.getInstance()),
                System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(Supplier<DatabaseInitializer> initializerFactory,
                   PrintStream output, PrintStream error) {
        Objects.requireNonNull(initializerFactory);
        Objects.requireNonNull(output);
        Objects.requireNonNull(error);
        try {
            initializerFactory.get().initialize();
            output.println("Classroom Booking System database is ready.");
            return 0;
        } catch (SQLException | IOException | IllegalStateException exception) {
            error.println("Unable to initialize the database. Start PostgreSQL with `docker compose up -d database`.");
            error.println(exception.getMessage());
            return 1;
        }
    }
}
