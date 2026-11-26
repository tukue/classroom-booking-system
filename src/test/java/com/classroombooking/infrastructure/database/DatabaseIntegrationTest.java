package com.classroombooking.infrastructure.database;

import com.classroombooking.Main;
import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.domain.model.CompanyCustomer;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.IndividualCustomer;
import com.classroombooking.domain.repository.CustomerRepository;
import com.classroombooking.infrastructure.persistence.JdbcCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
        try {
            System.setIn(new ByteArrayInputStream("0\n".getBytes(StandardCharsets.UTF_8)));
            Main.main(new String[0]);
        } finally {
            System.setIn(originalInput);
        }

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
                        OffsetDateTime.parse("2030-06-01T09:00:00Z"),
                        OffsetDateTime.parse("2030-06-01T10:00:00Z"));

                SQLException conflict = assertThrows(SQLException.class,
                        () -> insertBooking(connection, secondBookingId, customerId, bookingUserId,
                                classroomId,
                                OffsetDateTime.parse("2030-06-01T09:30:00Z"),
                                OffsetDateTime.parse("2030-06-01T10:30:00Z")));
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
            individual = service.registerIndividual("Pat Customer", "pat-" + suffix + "@example.test");

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
}
