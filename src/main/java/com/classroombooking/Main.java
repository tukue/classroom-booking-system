package com.classroombooking;

import com.classroombooking.application.classroom.ClassroomService;
import com.classroombooking.application.booking.BookingService;
import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.application.roomsearch.RoomRequirementsFactory;
import com.classroombooking.infrastructure.database.DatabaseInitializer;
import com.classroombooking.infrastructure.database.DatabaseManager;
import com.classroombooking.infrastructure.persistence.JdbcBookingAvailabilityRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingUserRepository;
import com.classroombooking.infrastructure.persistence.JdbcClassroomRepository;
import com.classroombooking.infrastructure.persistence.JdbcCustomerRepository;
import com.classroombooking.infrastructure.persistence.JdbcEquipmentRepository;
import com.classroombooking.presentation.console.ClassroomController;
import com.classroombooking.presentation.console.BookingController;
import com.classroombooking.presentation.console.ConsoleMenu;
import com.classroombooking.presentation.console.CustomerController;
import com.classroombooking.presentation.console.RoomSearchController;
import com.classroombooking.presentation.console.ConsoleView;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

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
            LOGGER.log(Level.FINE, "Application startup failed.", exception);
            error.println("Unable to initialize the database. Check its configuration and logs.");
            if (exception instanceof IllegalStateException) {
                error.println("Check that POSTGRES_PASSWORD is set in your .env file.");
            }
            return 1;
        }
        output.println("Classroom Booking System database is ready.");
        application.run();
        return 0;
    }

    private static void runConsoleMenu() {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        JdbcCustomerRepository customerRepository = new JdbcCustomerRepository(databaseManager);
        JdbcClassroomRepository classroomRepository = new JdbcClassroomRepository(databaseManager);
        JdbcEquipmentRepository equipmentRepository = new JdbcEquipmentRepository(databaseManager);
        JdbcBookingRepository bookingRepository = new JdbcBookingRepository(databaseManager);
        CustomerService customerService = new CustomerService(customerRepository);
        ClassroomService classroomService = new ClassroomService(classroomRepository, equipmentRepository);
        RoomRequirementsFactory requirementsFactory = new RoomRequirementsFactory(equipmentRepository);
        RoomSearchService roomSearchService = new RoomSearchService(
                classroomRepository,
                new JdbcBookingAvailabilityRepository(databaseManager));
        RoomSearchController roomSearchController = new RoomSearchController(roomSearchService, requirementsFactory);
        BookingService bookingService = new BookingService(bookingRepository,
                new JdbcBookingUserRepository(databaseManager), classroomRepository, customerRepository);
        BookingController bookingController = new BookingController(bookingService, requirementsFactory);
        ConsoleMenu menu = new ConsoleMenu(new ClassroomController(classroomService),
                new CustomerController(customerService),
                roomSearchController, bookingController, new ConsoleView(System.in, System.out));
        menu.run();
    }
}
