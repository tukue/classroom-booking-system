package com.classroombooking.domain.repository;

import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.BookingUser;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for booking writes, conflict checks, and schedule queries. */
public interface BookingRepository extends BookingScheduleRepository {
    Booking save(Booking booking, BookingUser bookingUser);

    Optional<Booking> findById(UUID bookingId);

    List<Booking> findOverlapping(UUID classroomId, OffsetDateTime startsAt, OffsetDateTime endsAt);
}
