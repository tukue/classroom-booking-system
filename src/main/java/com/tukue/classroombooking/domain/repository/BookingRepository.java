package com.tukue.classroombooking.domain.repository;

import com.tukue.classroombooking.domain.model.Booking;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for booking writes, conflict checks, and schedule queries. */
public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(UUID bookingId);

    List<Booking> findOverlapping(UUID classroomId, OffsetDateTime startsAt, OffsetDateTime endsAt);

    List<Booking> findUpcomingByClassroom(UUID classroomId, OffsetDateTime from);

    List<Booking> findUpcomingByCustomer(UUID customerId, OffsetDateTime from);

    List<Booking> findAllUpcoming(OffsetDateTime from);
}
