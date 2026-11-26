package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.repository.BookingScheduleRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcBookingScheduleRepository implements BookingScheduleRepository {
    private static final String SELECT = """
            SELECT booking_id, customer_id, booking_user_id, classroom_id, starts_at, ends_at, comments
            FROM bookings
            """;
    private static final String UPCOMING = " AND starts_at >= ? ORDER BY starts_at, booking_id";

    private final ConnectionFactory connections;

    public JdbcBookingScheduleRepository(ConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Booking> findUpcomingByClassroom(UUID classroomId, OffsetDateTime from) {
        return findUpcoming(SELECT + "WHERE classroom_id = ?" + UPCOMING, classroomId, from);
    }

    @Override
    public List<Booking> findUpcomingByCustomer(UUID customerId, OffsetDateTime from) {
        return findUpcoming(SELECT + "WHERE customer_id = ?" + UPCOMING, customerId, from);
    }

    private List<Booking> findUpcoming(String sql, UUID id, OffsetDateTime from) {
        try (var connection = connections.openConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setObject(2, from);
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
