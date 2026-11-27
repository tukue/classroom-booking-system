package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.booking.BookingConflictException;
import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.repository.BookingRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** JDBC booking persistence with transactional conflict checks. */
public final class JdbcBookingRepository implements BookingRepository {
    private static final Logger LOGGER = Logger.getLogger(JdbcBookingRepository.class.getName());
    private static final String BOOKING_COLUMNS = "booking_id, customer_id, booking_user_id, "
            + "classroom_id, starts_at, ends_at, comments";

    private final ConnectionFactory connectionFactory;

    public JdbcBookingRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    /** Rechecks room availability and inserts in one transaction; PostgreSQL protects concurrent inserts. */
    @Override
    public Booking save(Booking booking) {
        String sql = "INSERT INTO bookings (booking_id, customer_id, booking_user_id, classroom_id, "
                + "starts_at, ends_at, comments) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try {
                if (hasOverlap(connection, booking.classroomId(), booking.startsAt(), booking.endsAt())) {
                    throw new BookingConflictException(
                            "The selected classroom is already booked for that time.");
                }
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setObject(1, booking.id());
                    statement.setObject(2, booking.customerId());
                    statement.setObject(3, booking.bookingUserId());
                    statement.setObject(4, booking.classroomId());
                    statement.setObject(5, booking.startsAt());
                    statement.setObject(6, booking.endsAt());
                    statement.setString(7, booking.comments());
                    statement.executeUpdate();
                }
                connection.commit();
                return booking;
            } catch (SQLException exception) {
                rollback(connection, exception);
                if ("23P01".equals(exception.getSQLState())) {
                    throw new BookingConflictException(
                            "The selected classroom is already booked for that time.", exception);
                }
                LOGGER.log(Level.WARNING, "Could not save classroom booking.", exception);
                throw new PersistenceException("Booking could not be saved. Please try again.", exception);
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            LOGGER.log(Level.WARNING, "Could not open a transaction to save the booking.", exception);
            throw new PersistenceException("Booking could not be saved. Please try again.", exception);
        }
    }

    @Override
    public Optional<Booking> findById(UUID bookingId) {
        return query("SELECT " + BOOKING_COLUMNS + " FROM bookings WHERE booking_id = ?",
                statement -> statement.setObject(1, bookingId)).stream().findFirst();
    }

    @Override
    public List<Booking> findOverlapping(UUID classroomId, OffsetDateTime startsAt,
                                         OffsetDateTime endsAt) {
        String sql = "SELECT " + BOOKING_COLUMNS + " FROM bookings "
                + "WHERE classroom_id = ? AND starts_at < ? AND ends_at > ? ORDER BY starts_at";
        return query(sql, statement -> {
            statement.setObject(1, classroomId);
            statement.setObject(2, endsAt);
            statement.setObject(3, startsAt);
        });
    }

    @Override
    public List<Booking> findUpcomingByClassroom(UUID classroomId, OffsetDateTime from) {
        String sql = "SELECT " + BOOKING_COLUMNS + " FROM bookings "
                + "WHERE classroom_id = ? AND starts_at >= ? ORDER BY starts_at";
        return query(sql, statement -> {
            statement.setObject(1, classroomId);
            statement.setObject(2, from);
        });
    }

    @Override
    public List<Booking> findUpcomingByCustomer(UUID customerId, OffsetDateTime from) {
        String sql = "SELECT " + BOOKING_COLUMNS + " FROM bookings "
                + "WHERE customer_id = ? AND starts_at >= ? ORDER BY starts_at";
        return query(sql, statement -> {
            statement.setObject(1, customerId);
            statement.setObject(2, from);
        });
    }

    @Override
    public List<Booking> findAllUpcoming(OffsetDateTime from) {
        return query("SELECT " + BOOKING_COLUMNS + " FROM bookings WHERE starts_at >= ? ORDER BY starts_at",
                statement -> statement.setObject(1, from));
    }

    private boolean hasOverlap(Connection connection, UUID classroomId,
                               OffsetDateTime startsAt, OffsetDateTime endsAt) throws SQLException {
        String sql = "SELECT 1 FROM bookings WHERE classroom_id = ? "
                + "AND starts_at < ? AND ends_at > ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, classroomId);
            statement.setObject(2, endsAt);
            statement.setObject(3, startsAt);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private List<Booking> query(String sql, StatementBinder binder) {
        List<Booking> bookings = new ArrayList<>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bookings.add(mapBooking(resultSet));
                }
            }
            return List.copyOf(bookings);
        } catch (SQLException exception) {
            LOGGER.log(Level.FINE, "Could not retrieve booking data.", exception);
            throw new PersistenceException("Booking data could not be retrieved. Please try again.", exception);
        }
    }

    private Booking mapBooking(ResultSet resultSet) throws SQLException {
        return new Booking(
                resultSet.getObject("booking_id", UUID.class),
                resultSet.getObject("customer_id", UUID.class),
                resultSet.getObject("booking_user_id", UUID.class),
                resultSet.getObject("classroom_id", UUID.class),
                resultSet.getObject("starts_at", OffsetDateTime.class),
                resultSet.getObject("ends_at", OffsetDateTime.class),
                resultSet.getString("comments"));
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
