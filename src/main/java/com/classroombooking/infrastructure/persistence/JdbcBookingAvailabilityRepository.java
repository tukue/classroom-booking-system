package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.repository.BookingAvailabilityRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Reads existing room bookings to exclude occupied classrooms from search results. */
public final class JdbcBookingAvailabilityRepository implements BookingAvailabilityRepository {
    private static final Logger LOGGER = Logger.getLogger(JdbcBookingAvailabilityRepository.class.getName());
    private final ConnectionFactory connectionFactory;

    public JdbcBookingAvailabilityRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public List<Booking> findOverlapping(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        String sql = "SELECT booking_id, customer_id, booking_user_id, classroom_id, starts_at, "
                + "ends_at, comments FROM bookings WHERE starts_at < ? AND ends_at > ?";
        List<Booking> bookings = new ArrayList<>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, endsAt);
            statement.setObject(2, startsAt);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bookings.add(mapBooking(resultSet));
                }
            }
            return List.copyOf(bookings);
        } catch (SQLException exception) {
            LOGGER.log(Level.WARNING, "Could not check classroom booking overlaps.", exception);
            throw new PersistenceException("Room availability could not be checked. Please try again.",
                    exception);
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
}
