package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.repository.BookingScheduleRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class JdbcBookingScheduleRepository implements BookingScheduleRepository {
    private static final String SELECT = """
            SELECT booking_id, customer_id, booking_user_id, classroom_id, starts_at, ends_at, comments
            FROM bookings
            WHERE starts_at >= ?
            """;
    private static final String START_TIME_ORDER = " ORDER BY starts_at, booking_id";

    private final ConnectionFactory connections;

    public JdbcBookingScheduleRepository(ConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Booking> findAllUpcoming(OffsetDateTime from) {
        return findUpcoming("", from, null);
    }

    @Override
    public List<Booking> findUpcomingByClassroom(UUID classroomId, OffsetDateTime from) {
        return findUpcoming(" AND classroom_id = ?", from, Objects.requireNonNull(classroomId));
    }

    @Override
    public List<Booking> findUpcomingByCustomer(UUID customerId, OffsetDateTime from) {
        return findUpcoming(" AND customer_id = ?", from, Objects.requireNonNull(customerId));
    }

    private List<Booking> findUpcoming(String filter, OffsetDateTime from, UUID id) {
        try (var connection = connections.openConnection();
             var statement = connection.prepareStatement(SELECT + filter + START_TIME_ORDER)) {
            statement.setObject(1, from);
            if (id != null) {
                statement.setObject(2, id);
            }
            try (var rows = statement.executeQuery()) {
                List<Booking> bookings = new ArrayList<>();
                while (rows.next()) {
                    bookings.add(new Booking(
                            rows.getObject("booking_id", UUID.class),
                            rows.getObject("customer_id", UUID.class),
                            rows.getObject("booking_user_id", UUID.class),
                            rows.getObject("classroom_id", UUID.class),
                            rows.getObject("starts_at", OffsetDateTime.class),
                            rows.getObject("ends_at", OffsetDateTime.class),
                            rows.getString("comments")));
                }
                return List.copyOf(bookings);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Upcoming bookings could not be loaded.", exception);
        }
    }
}
