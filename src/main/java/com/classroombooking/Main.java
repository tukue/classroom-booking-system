package com.classroombooking;

import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.infrastructure.database.DatabaseInitializer;
import com.classroombooking.infrastructure.database.DatabaseManager;
import com.classroombooking.infrastructure.persistence.JdbcCustomerRepository;
import com.classroombooking.presentation.console.ConsoleMenu;
import com.classroombooking.presentation.console.CustomerController;
import com.classroombooking.presentation.console.ConsoleView;

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
                Main::runConsoleMenu, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(Supplier<DatabaseInitializer> initializerFactory, Runnable application,
                   PrintStream output, PrintStream error) {
        Objects.requireNonNull(initializerFactory);
        Objects.requireNonNull(application);
        Objects.requireNonNull(output);
        Objects.requireNonNull(error);
        try {
            initializerFactory.get().initialize();
        } catch (SQLException | IOException | IllegalStateException exception) {
            error.println("Unable to initialize the database. Start PostgreSQL with `docker compose up -d database`.");
            error.println(exception.getMessage());
            return 1;
        }
        output.println("Classroom Booking System database is ready.");
        application.run();
        return 0;
    }

    private static void runConsoleMenu() {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        CustomerService customerService = new CustomerService(
                new JdbcCustomerRepository(databaseManager));
        ConsoleMenu menu = new ConsoleMenu(
                new CustomerController(customerService),
                new ConsoleView(System.in, System.out));
        menu.run();
    }
}
