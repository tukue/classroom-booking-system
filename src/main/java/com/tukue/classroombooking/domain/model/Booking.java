package com.tukue.classroombooking.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** A time reservation linking a classroom, customer, and creating booking user. */
public record Booking(UUID id, UUID customerId, UUID bookingUserId, UUID classroomId,
                      OffsetDateTime startsAt, OffsetDateTime endsAt, String comments) {
    public Booking {
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Booking start must be before its end.");
        }
    }

    /** Returns whether this booking overlaps another half-open time interval. */
    public boolean overlaps(OffsetDateTime otherStartsAt, OffsetDateTime otherEndsAt) {
        return startsAt.isBefore(otherEndsAt) && otherStartsAt.isBefore(endsAt);
    }
}
