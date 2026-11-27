package com.classroombooking.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/** Validated request to reserve a selected classroom for a customer. */
public record BookingRequest(UUID customerId, String bookingUserName, String bookingUserEmail,
                             UUID classroomId, OffsetDateTime startsAt, OffsetDateTime endsAt,
                             RoomRequirements requirements, String comments) {
    public BookingRequest {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(classroomId, "classroomId");
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        Objects.requireNonNull(requirements, "requirements");
        bookingUserName = requireText(bookingUserName, "Booking user name");
        bookingUserEmail = requireText(bookingUserEmail, "Booking user email");
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Booking start must be before its end.");
        }
        comments = comments == null || comments.isBlank() ? null : comments.strip();
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
        return value.strip();
    }
}
