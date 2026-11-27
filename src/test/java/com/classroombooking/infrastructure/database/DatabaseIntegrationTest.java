package com.classroombooking.infrastructure.database;

import com.classroombooking.Main;
import com.classroombooking.application.booking.BookingConflictException;
import com.classroombooking.application.booking.BookingService;
import com.classroombooking.application.classroom.ClassroomService;
import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.application.roomsearch.RoomRequirementsFactory;
import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.BookingRequest;
import com.classroombooking.domain.model.BookingUser;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.CompanyCustomer;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.IndividualCustomer;
import com.classroombooking.domain.repository.BookingRepository;
import com.classroombooking.domain.repository.BookingUserRepository;
import com.classroombooking.domain.repository.CustomerRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingUserRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingScheduleRepository;
import com.classroombooking.infrastructure.persistence.JdbcClassroomRepository;
import com.classroombooking.infrastructure.persistence.JdbcCustomerRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingAvailabilityRepository;
import com.classroombooking.infrastructure.persistence.JdbcEquipmentRepository;
import com.classroombooking.presentation.console.BookingScheduleController;
import com.classroombooking.presentation.console.ConsoleMenu;
import com.classroombooking.presentation.console.ClassroomController;
import com.classroombooking.presentation.console.CustomerController;
import com.classroombooking.presentation.console.RoomSearchController;
import com.classroombooking.presentation.console.ConsoleView;
import com.classroombooking.presentation.console.BookingController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "db.integration", matches = "true")
class DatabaseIntegrationTest {
    @Test
    void schemaInitializationIsRepeatableAndSeedsTwentyClassrooms() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        assertSame(databaseManager, DatabaseManager.getInstance());
        DatabaseInitializer initializer = new DatabaseInitializer(databaseManager);

        initializer.initialize();
        initializer.initialize();
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        PrintStream originalError = System.err;
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8);
             PrintStream error = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(output);
            System.setErr(error);
            Main.main(new String[0]);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
            System.setErr(originalError);
        }
        assertTrue(outputBytes.toString(StandardCharsets.UTF_8)
                .contains("Classroom Booking System database is ready."));

        try (Connection first = databaseManager.openConnection();
             Connection second = databaseManager.openConnection()) {
            assertNotSame(first, second);
        }

        try (Connection connection = databaseManager.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM classrooms")) {
            resultSet.next();
            assertEquals(20, resultSet.getInt(1));
        }

        try (Connection connection = databaseManager.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT count(*) FROM classroom_equipment")) {
            resultSet.next();
            assertEquals(31, resultSet.getInt(1));
        }
    }

    @Test
    void relationalConstraintsLinkBookingsAndRejectOverlappingRoomTimes() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        new DatabaseInitializer(databaseManager).initialize();

        UUID customerId = UUID.randomUUID();
        UUID bookingUserId = UUID.randomUUID();
        UUID firstBookingId = UUID.randomUUID();
        UUID secondBookingId = UUID.randomUUID();
        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                insertCustomer(connection, customerId);
                insertBookingUser(connection, bookingUserId);
                UUID classroomId = findFirstClassroomId(connection);
                insertBooking(connection, firstBookingId, customerId, bookingUserId, classroomId,
                        OffsetDateTime.parse("2026-12-01T09:00:00Z"),
                        OffsetDateTime.parse("2026-12-01T10:00:00Z"));

                SQLException conflict = assertThrows(SQLException.class,
                        () -> insertBooking(connection, secondBookingId, customerId, bookingUserId,
                                classroomId,
                                OffsetDateTime.parse("2026-12-01T09:30:00Z"),
                                OffsetDateTime.parse("2026-12-01T10:30:00Z")));
                assertEquals("23P01", conflict.getSQLState());
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void rejectsDuplicateCustomerEmails() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        new DatabaseInitializer(databaseManager).initialize();

        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                String email = "duplicate-" + UUID.randomUUID() + "@example.test";
                insertCustomer(connection, UUID.randomUUID(), email);
                SQLException duplicate = assertThrows(SQLException.class,
                        () -> insertCustomer(connection, UUID.randomUUID(), email));
                assertEquals("23505", duplicate.getSQLState());
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void customerRepositoryReturnsSafeMessageForDatabaseConstraintErrors() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        new DatabaseInitializer(databaseManager).initialize();
        CustomerRepository repository = new JdbcCustomerRepository(databaseManager);
        String email = "safe-error-" + UUID.randomUUID() + "@example.test";
        Customer customer = new CompanyCustomer(UUID.randomUUID(), "Example Customer", email);

        repository.save(customer);
        try {
            PersistenceException exception = assertThrows(PersistenceException.class,
                    () -> repository.save(new IndividualCustomer(UUID.randomUUID(),
                            "Duplicate Customer", email)));

            assertEquals("Customer could not be saved. Please try again.", exception.getMessage());
            assertTrue(exception.getCause() instanceof SQLException);
            assertTrue(!exception.getMessage().contains("customers_email_key"));
        } finally {
            repository.deleteById(customer.id());
        }
    }

    @Test
    void rejectsDuplicateBookingUserEmails() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        new DatabaseInitializer(databaseManager).initialize();

        try (Connection connection = databaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                String email = "duplicate-" + UUID.randomUUID() + "@example.test";
                insertBookingUser(connection, UUID.randomUUID(), email);
                SQLException duplicate = assertThrows(SQLException.class,
                        () -> insertBookingUser(connection, UUID.randomUUID(), email));
                assertEquals("23505", duplicate.getSQLState());
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void customerRegistrationLoadsUpdatesAndDeletesBothCustomerTypes() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        new DatabaseInitializer(databaseManager).initialize();
        CustomerRepository repository = new JdbcCustomerRepository(databaseManager);
        CustomerService service = new CustomerService(repository);
        int existingCustomerCount = repository.findAll().size();
        String suffix = UUID.randomUUID().toString();

        Customer company = service.registerCompany("Acme Ltd", "acme-" + suffix + "@example.test");
        Customer individual = null;
        try {
            individual = service.registerIndividual("Alex Customer", "alex-" + suffix + "@example.test");

            assertInstanceOf(CompanyCustomer.class, repository.findById(company.id()).orElseThrow());
            assertInstanceOf(IndividualCustomer.class, repository.findById(individual.id()).orElseThrow());
            assertEquals(existingCustomerCount + 2, service.findAllCustomers().size());

            Customer updatedCompany = new CompanyCustomer(company.id(), "Acme Training Ltd",
                    "training-" + suffix + "@acme.example.test");
            repository.update(updatedCompany);
            assertEquals(updatedCompany, repository.findById(company.id()).orElseThrow());

            repository.deleteById(individual.id());
            assertTrue(repository.findById(individual.id()).isEmpty());
        } finally {
            if (individual != null) {
                repository.deleteById(individual.id());
            }
            repository.deleteById(company.id());
        }
    }

    @Test
    void consoleCustomerRegistrationPersistsAndDisplaysCompanyAndIndividual() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        new DatabaseInitializer(databaseManager).initialize();

        CustomerRepository repository = new JdbcCustomerRepository(databaseManager);
        CustomerController controller = new CustomerController(new CustomerService(repository));
        RoomSearchService roomSearchService = new RoomSearchService(
                new JdbcClassroomRepository(databaseManager),
                new JdbcBookingAvailabilityRepository(databaseManager));
        RoomSearchController roomSearchController = new RoomSearchController(roomSearchService,
                new JdbcEquipmentRepository(databaseManager));
        String suffix = UUID.randomUUID().toString();
        String companyEmail = "console-company-" + suffix + "@example.test";
        String individualEmail = "console-individual-" + suffix + "@example.test";
        String menuInput = "1\n1\nConsole Company\n" + companyEmail + "\n"
                + "1\n2\nConsole Individual\n" + individualEmail + "\n2\n0\n";
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8)) {
            ConsoleView view = new ConsoleView(
                    new ByteArrayInputStream(menuInput.getBytes(StandardCharsets.UTF_8)), output);
            new ConsoleMenu(createClassroomController(databaseManager), controller,
                    roomSearchController, createBookingController(databaseManager), view).run();
        }

        try {
            List<Customer> persistedCustomers = new JdbcCustomerRepository(databaseManager).findAll()
                    .stream()
                    .filter(customer -> customer.email().equals(companyEmail)
                            || customer.email().equals(individualEmail))
                    .toList();

            assertEquals(2, persistedCustomers.size());
            assertTrue(persistedCustomers.stream().anyMatch(CompanyCustomer.class::isInstance));
            assertTrue(persistedCustomers.stream().anyMatch(IndividualCustomer.class::isInstance));
            assertTrue(outputBytes.toString(StandardCharsets.UTF_8).contains(companyEmail));
            assertTrue(outputBytes.toString(StandardCharsets.UTF_8).contains(individualEmail));
        } finally {
            new JdbcCustomerRepository(databaseManager).findAll().stream()
                    .filter(customer -> customer.email().equals(companyEmail)
                            || customer.email().equals(individualEmail))
                    .forEach(customer -> repository.deleteById(customer.id()));
        }
    }

    @Test
    void bookingViewsShowUpcomingBookingsInOrderWithTheRequestedScope() throws Exception {
        DatabaseManager database = DatabaseManager.getInstance();
        new DatabaseInitializer(database).initialize();
        var classrooms = new JdbcClassroomRepository(database);
        UUID room = UUID.randomUUID();
        UUID otherRoom = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        UUID otherCustomer = UUID.randomUUID();
        UUID bookingUser = UUID.randomUUID();
        UUID roomFirst = UUID.randomUUID();
        UUID roomLast = UUID.randomUUID();
        UUID customerFirst = UUID.randomUUID();
        UUID past = UUID.randomUUID();
        UUID unrelated = UUID.randomUUID();
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
        OffsetDateTime tomorrow = cutoff.plusDays(1);

        try (Connection connection = database.openConnection()) {
            classrooms.save(new Classroom(room, "Schedule room " + room, 10, Set.of(), false));
            classrooms.save(new Classroom(otherRoom, "Schedule room " + otherRoom, 10, Set.of(), false));
            try {
                insertCustomer(connection, customer);
                insertCustomer(connection, otherCustomer);
                insertBookingUser(connection, bookingUser);
                insertBooking(connection, roomLast, customer, bookingUser, room,
                        tomorrow.plusHours(2), tomorrow.plusHours(3));
                insertBooking(connection, roomFirst, otherCustomer, bookingUser, room, tomorrow, tomorrow.plusHours(1));
                insertBooking(connection, customerFirst, customer, bookingUser, otherRoom,
                        tomorrow.plusMinutes(30), tomorrow.plusHours(1));
                insertBooking(connection, past, customer, bookingUser, room, cutoff.minusDays(1), cutoff.minusHours(1));
                insertBooking(connection, unrelated, otherCustomer, bookingUser, otherRoom,
                        tomorrow.plusHours(3), tomorrow.plusHours(4));

                var repository = new JdbcBookingScheduleRepository(database);
                assertEquals(List.of(roomFirst, roomLast),
                        repository.findUpcomingByClassroom(room, tomorrow).stream().map(Booking::id).toList());
                assertEquals(List.of(customerFirst, roomLast),
                        repository.findUpcomingByCustomer(customer, cutoff).stream().map(Booking::id).toList());
                assertEquals(List.of(), repository.findUpcomingByClassroom(UUID.randomUUID(), cutoff));

                List<UUID> expected = List.of(roomFirst, customerFirst, roomLast, unrelated);
                assertEquals(expected, repository.findAllUpcoming(tomorrow).stream()
                        .filter(booking -> booking.bookingUserId().equals(bookingUser)).map(Booking::id).toList());

                String allUpcoming = runConsole("10\n0\n");
                int previousPosition = -1;
                for (UUID id : expected) {
                    int position = allUpcoming.indexOf(id.toString());
                    assertTrue(position > previousPosition, allUpcoming);
                    previousPosition = position;
                }
                assertTrue(!allUpcoming.contains(past.toString()), allUpcoming);

                var scheduleOnlyOutput = new ByteArrayOutputStream();
                try (var output = new PrintStream(scheduleOnlyOutput, true, StandardCharsets.UTF_8)) {
                    var view = new ConsoleView(new ByteArrayInputStream("10\n0\n".getBytes(StandardCharsets.UTF_8)), output);
                    var customers = new CustomerController(new CustomerService(new JdbcCustomerRepository(database)));
                    var schedules = new BookingScheduleController(repository, Clock.fixed(cutoff.toInstant(), ZoneOffset.UTC));
                    new ConsoleMenu(null, customers, null, schedules, view).run();
                }
                String scheduleOnly = scheduleOnlyOutput.toString(StandardCharsets.UTF_8);
                assertTrue(scheduleOnly.contains("10. View all upcoming bookings"), scheduleOnly);
                assertBookingOutput(scheduleOnly, roomFirst, unrelated, past);
                assertBookingOutput(runScheduleOption(8, room), roomFirst, roomLast, customerFirst, past, unrelated);
                assertBookingOutput(runScheduleOption(9, customer), customerFirst, roomLast, roomFirst, past, unrelated);
                assertTrue(runScheduleOption(8, UUID.randomUUID()).contains("Classroom not found."));
            } finally {
                executeDelete(connection, "DELETE FROM bookings WHERE booking_user_id = ?", bookingUser);
                executeDelete(connection, "DELETE FROM booking_users WHERE booking_user_id = ?", bookingUser);
                executeDelete(connection, "DELETE FROM customers WHERE customer_id = ?", customer);
                executeDelete(connection, "DELETE FROM customers WHERE customer_id = ?", otherCustomer);
                classrooms.deleteById(room);
                classrooms.deleteById(otherRoom);
            }
        }
    }

    private String runScheduleOption(int option, UUID selection) {
        return runConsole(option + "\n" + selection + "\n0\n");
    }

    private String runConsole(String commands) {
        var input = new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        try (var capture = new PrintStream(output, true, StandardCharsets.UTF_8);
             var error = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            assertEquals(0, Main.run(input, capture, error), () -> errors.toString(StandardCharsets.UTF_8));
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private void assertBookingOutput(String output, UUID first, UUID second, UUID... excluded) {
        int firstPosition = output.indexOf(first.toString());
        assertTrue(firstPosition >= 0 && output.indexOf(second.toString()) > firstPosition, output);
        for (UUID id : excluded) {
            assertTrue(!output.contains(id.toString()), output);
        }
    }

    private void executeDelete(Connection connection, String sql, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.executeUpdate();
        }
    }

    @Test
    void consoleRoomSearchFiltersByRoomRequirementsAndOverlappingBookings() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        new DatabaseInitializer(databaseManager).initialize();

        UUID customerId = UUID.randomUUID();
        UUID bookingUserId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        try {
            try (Connection connection = databaseManager.openConnection()) {
                insertCustomer(connection, customerId);
                insertBookingUser(connection, bookingUserId);
                UUID classroomId = findClassroomId(connection, "Training Room 05");
                insertBooking(connection, bookingId, customerId, bookingUserId, classroomId,
                        OffsetDateTime.parse("2026-12-02T09:00:00Z"),
                        OffsetDateTime.parse("2026-12-02T10:00:00Z"));
            }

            RoomSearchService roomSearchService = new RoomSearchService(
                    new JdbcClassroomRepository(databaseManager),
                    new JdbcBookingAvailabilityRepository(databaseManager));
            RoomSearchController roomSearchController = new RoomSearchController(roomSearchService,
                    new JdbcEquipmentRepository(databaseManager));
            CustomerController customerController = new CustomerController(
                    new CustomerService(new JdbcCustomerRepository(databaseManager)));
            String input = "7\n2026-12-02T09:30:00Z\n2026-12-02T10:30:00Z\n"
                    + "20\nPROJECTOR\ny\n0\n";
            ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
            try (PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8)) {
                ConsoleView view = new ConsoleView(
                        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output);
                new ConsoleMenu(createClassroomController(databaseManager), customerController,
                        roomSearchController, createBookingController(databaseManager), view).run();
            }

            String result = outputBytes.toString(StandardCharsets.UTF_8);
            assertTrue(result.contains("Training Room 07"));
            assertTrue(!result.contains("Training Room 05"));
        } finally {
            deleteBookingFixtures(databaseManager, bookingId, bookingUserId, customerId);
        }
    }

    @Test
    void consoleBookingPersistsAssociationsCommentsAndRejectsConflicts() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        new DatabaseInitializer(databaseManager).initialize();
        JdbcCustomerRepository customers = new JdbcCustomerRepository(databaseManager);
        JdbcClassroomRepository classrooms = new JdbcClassroomRepository(databaseManager);
        JdbcEquipmentRepository equipmentRepository = new JdbcEquipmentRepository(databaseManager);
        RoomRequirementsFactory requirementsFactory = new RoomRequirementsFactory(equipmentRepository);
        BookingRepository bookings = new JdbcBookingRepository(databaseManager);
        BookingUserRepository bookingUsers = new JdbcBookingUserRepository(databaseManager);

        String suffix = UUID.randomUUID().toString();
        Customer customer = customers.save(new CompanyCustomer(UUID.randomUUID(),
                "Booking E2E " + suffix, "booking-e2e-" + suffix + "@example.test"));
        Classroom room = classrooms.findAll().stream()
                .filter(classroom -> classroom.name().equals("Training Room 07"))
                .findFirst().orElseThrow();
        OffsetDateTime startsAt = OffsetDateTime.parse("2026-12-03T09:00:00Z");
        OffsetDateTime endsAt = OffsetDateTime.parse("2026-12-03T10:00:00Z");
        String bookingUserEmail = "booking-user-" + suffix + "@example.test";
        BookingService service = new BookingService(bookings, bookingUsers, classrooms, customers);
        BookingController bookingController = new BookingController(service, requirementsFactory);
        RoomSearchController searchController = new RoomSearchController(
                new RoomSearchService(classrooms, new JdbcBookingAvailabilityRepository(databaseManager)),
                requirementsFactory);
        ClassroomController classroomController = createClassroomController(databaseManager);
        CustomerController customerController = new CustomerController(new CustomerService(customers));
        String input = "11\n" + customer.id() + "\nBooking Operator\n" + bookingUserEmail + "\n"
                + startsAt + "\n" + endsAt + "\n20\nPROJECTOR\ny\n" + room.id()
                + "\nAllergy: peanuts\n0\n";
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();

        try {
            try (PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8)) {
                ConsoleView view = new ConsoleView(
                        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output);
                new ConsoleMenu(classroomController, customerController, searchController,
                        bookingController, view).run();
            }

            List<Booking> customerBookings = bookings.findUpcomingByCustomer(customer.id(), startsAt.minusDays(1));
            assertEquals(1, customerBookings.size());
            Booking savedBooking = customerBookings.getFirst();
            assertEquals(customer.id(), savedBooking.customerId());
            assertEquals(room.id(), savedBooking.classroomId());
            assertEquals(startsAt, savedBooking.startsAt());
            assertEquals(endsAt, savedBooking.endsAt());
            assertEquals("Allergy: peanuts", savedBooking.comments());
            assertEquals(savedBooking, bookings.findById(savedBooking.id()).orElseThrow());
            assertTrue(bookings.findUpcomingByClassroom(room.id(), startsAt.minusDays(1))
                    .contains(savedBooking));
            assertTrue(bookings.findAllUpcoming(startsAt.minusDays(1)).contains(savedBooking));

            BookingUser bookingUser = bookingUsers.findByEmail(bookingUserEmail).orElseThrow();
            assertEquals("Booking Operator", bookingUser.name());
            assertEquals(bookingUser.id(), savedBooking.bookingUserId());
            assertTrue(outputBytes.toString(StandardCharsets.UTF_8).contains("Booking created:"));

            BookingRequest conflictingRequest = new BookingRequest(customer.id(), "Booking Operator",
                    bookingUserEmail, room.id(), startsAt, endsAt,
                    requirementsFactory.create(20, "PROJECTOR", true), null);
            assertThrows(BookingConflictException.class,
                    () -> service.createBooking(conflictingRequest));

            BookingUser unusedBookingUser = new BookingUser(UUID.randomUUID(), "Unused Operator",
                    "unused-booking-user-" + suffix + "@example.test");
            Booking conflictingBooking = new Booking(UUID.randomUUID(), customer.id(),
                    unusedBookingUser.id(), room.id(), startsAt, endsAt, null);
            assertThrows(BookingConflictException.class,
                    () -> bookings.save(conflictingBooking, unusedBookingUser));
            assertTrue(bookingUsers.findByEmail(unusedBookingUser.email()).isEmpty());

            BookingRequest adjacentRequest = new BookingRequest(customer.id(), "Booking Operator",
                    bookingUserEmail, room.id(), endsAt, endsAt.plusHours(1),
                    requirementsFactory.create(20, "PROJECTOR", true), null);
            Booking adjacentBooking = service.createBooking(adjacentRequest);
            assertNull(bookings.findById(adjacentBooking.id()).orElseThrow().comments());
            assertEquals(bookingUser.id(), adjacentBooking.bookingUserId());
        } finally {
            deleteBookingFixturesForCustomer(databaseManager, customer.id(), bookingUserEmail);
        }
    }

    @Test
    void classroomLookupHelperReportsMissingSeededRoomClearly() throws Exception {
        new DatabaseInitializer(DatabaseManager.getInstance()).initialize();

        try (Connection connection = DatabaseManager.getInstance().openConnection()) {
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> findClassroomId(connection, "Missing Training Room"));
            assertEquals("Classroom not found in seed data: Missing Training Room", exception.getMessage());
        }
    }

    private void insertCustomer(Connection connection, UUID customerId) throws SQLException {
        insertCustomer(connection, customerId, customerId + "@example.test");
    }

    private void insertCustomer(Connection connection, UUID customerId, String email)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO customers (customer_id, customer_type, display_name, email) "
                        + "VALUES (?, 'COMPANY', 'Integration Test Customer', ?)")) {
            statement.setObject(1, customerId);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    private void insertBookingUser(Connection connection, UUID bookingUserId) throws SQLException {
        insertBookingUser(connection, bookingUserId, bookingUserId + "@example.test");
    }

    private void insertBookingUser(Connection connection, UUID bookingUserId, String email)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO booking_users (booking_user_id, name, email) VALUES (?, ?, ?)")) {
            statement.setObject(1, bookingUserId);
            statement.setString(2, "Integration Test User");
            statement.setString(3, email);
            statement.executeUpdate();
        }
    }

    private UUID findFirstClassroomId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT classroom_id FROM classrooms ORDER BY name LIMIT 1")) {
            resultSet.next();
            return resultSet.getObject(1, UUID.class);
        }
    }

    private ClassroomController createClassroomController(DatabaseManager databaseManager) {
        return new ClassroomController(new ClassroomService(
                new JdbcClassroomRepository(databaseManager),
                new JdbcEquipmentRepository(databaseManager)));
    }

    private BookingController createBookingController(DatabaseManager databaseManager) {
        JdbcCustomerRepository customers = new JdbcCustomerRepository(databaseManager);
        JdbcClassroomRepository classrooms = new JdbcClassroomRepository(databaseManager);
        JdbcEquipmentRepository equipment = new JdbcEquipmentRepository(databaseManager);
        RoomRequirementsFactory requirements = new RoomRequirementsFactory(equipment);
        BookingService service = new BookingService(new JdbcBookingRepository(databaseManager),
                new JdbcBookingUserRepository(databaseManager), classrooms, customers);
        return new BookingController(service, requirements);
    }

    private UUID findClassroomId(Connection connection, String classroomName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT classroom_id FROM classrooms WHERE name = ?")) {
            statement.setString(1, classroomName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Classroom not found in seed data: " + classroomName);
                }
                return resultSet.getObject(1, UUID.class);
            }
        }
    }

    private void insertBooking(Connection connection, UUID bookingId, UUID customerId,
                               UUID bookingUserId, UUID classroomId, OffsetDateTime startsAt,
                               OffsetDateTime endsAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bookings (booking_id, customer_id, booking_user_id, classroom_id, "
                        + "starts_at, ends_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setObject(1, bookingId);
            statement.setObject(2, customerId);
            statement.setObject(3, bookingUserId);
            statement.setObject(4, classroomId);
            statement.setObject(5, startsAt);
            statement.setObject(6, endsAt);
            statement.executeUpdate();
        }
    }

    private void deleteBookingFixtures(DatabaseManager databaseManager, UUID bookingId,
                                       UUID bookingUserId, UUID customerId) throws SQLException {
        try (Connection connection = databaseManager.openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM bookings WHERE booking_id = ?")) {
                statement.setObject(1, bookingId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM booking_users WHERE booking_user_id = ?")) {
                statement.setObject(1, bookingUserId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM customers WHERE customer_id = ?")) {
                statement.setObject(1, customerId);
                statement.executeUpdate();
            }
        }
    }

    private void deleteBookingFixturesForCustomer(DatabaseManager databaseManager, UUID customerId,
                                                   String bookingUserEmail) throws SQLException {
        try (Connection connection = databaseManager.openConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM bookings WHERE customer_id = ?")) {
                statement.setObject(1, customerId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM booking_users WHERE email = ?")) {
                statement.setString(1, bookingUserEmail);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM customers WHERE customer_id = ?")) {
                statement.setObject(1, customerId);
                statement.executeUpdate();
            }
        }
    }
}
