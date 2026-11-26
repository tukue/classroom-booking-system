package com.classroombooking.domain.repository;

import com.classroombooking.domain.model.Booking;

import java.time.OffsetDateTime;
import java.util.List;

/** Persistence port for booking writes, conflict checks, and schedule queries. */
public interface BookingRepository {
    List<Booking> findOverlapping(OffsetDateTime startsAt, OffsetDateTime endsAt);
}
