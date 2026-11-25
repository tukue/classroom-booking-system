package com.tukue.classroombooking.infrastructure.database;

import com.tukue.classroombooking.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfSystemProperty(named = "db.integration", matches = "true")
class DatabaseIntegrationTest {
    @Test
    void schemaInitializationIsRepeatableAndSeedsTwentyClassrooms() throws Exception {
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        assertSame(databaseManager, DatabaseManager.getInstance());
        DatabaseInitializer initializer = new DatabaseInitializer(databaseManager);

        initializer.initialize();
        initializer.initialize();
        Main.main(new String[0]);

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

    private void insertCustomer(Connection connection, UUID customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO customers (customer_id, customer_type, display_name, email) "
                        + "VALUES (?, 'COMPANY', 'Integration Test Customer', ?)")) {
            statement.setObject(1, customerId);
            statement.setString(2, customerId + "@example.test");
            statement.executeUpdate();
        }
    }

    private void insertBookingUser(Connection connection, UUID bookingUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO booking_users (booking_user_id, name, email) VALUES (?, ?, ?)")) {
            statement.setObject(1, bookingUserId);
            statement.setString(2, "Integration Test User");
            statement.setString(3, bookingUserId + "@example.test");
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
