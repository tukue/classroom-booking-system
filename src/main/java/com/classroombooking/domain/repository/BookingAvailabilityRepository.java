package com.classroombooking.domain.repository;

import com.classroombooking.domain.model.Booking;

import java.time.OffsetDateTime;
import java.util.List;

/** Read port for room-search queries over existing bookings. */
public interface BookingAvailabilityRepository {
    List<Booking> findOverlapping(OffsetDateTime startsAt, OffsetDateTime endsAt);
}
