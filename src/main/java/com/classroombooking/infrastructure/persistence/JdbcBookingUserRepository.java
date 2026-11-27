package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.BookingUser;
import com.classroombooking.domain.repository.BookingUserRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** JDBC persistence for booking users linked to reservations. */
public final class JdbcBookingUserRepository implements BookingUserRepository {
    private final ConnectionFactory connectionFactory;

    public JdbcBookingUserRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public BookingUser save(BookingUser bookingUser) {
        String sql = "INSERT INTO booking_users (booking_user_id, name, email) VALUES (?, ?, ?)";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, bookingUser.id());
            statement.setString(2, bookingUser.name());
            statement.setString(3, bookingUser.email());
            statement.executeUpdate();
            return bookingUser;
        } catch (SQLException exception) {
            throw new PersistenceException("Booking user could not be saved. Please check the details.",
                    exception);
        }
    }

    @Override
    public Optional<BookingUser> findById(UUID bookingUserId) {
        return findOne("SELECT booking_user_id, name, email FROM booking_users WHERE booking_user_id = ?",
                bookingUserId);
    }

    @Override
    public Optional<BookingUser> findByEmail(String email) {
        return findOne("SELECT booking_user_id, name, email FROM booking_users WHERE email = ?", email);
    }

    private Optional<BookingUser> findOne(String sql, Object value) {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BookingUser(
                        resultSet.getObject("booking_user_id", UUID.class),
                        resultSet.getString("name"), resultSet.getString("email")));
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Booking user could not be retrieved. Please try again.",
                    exception);
        }
    }
}
