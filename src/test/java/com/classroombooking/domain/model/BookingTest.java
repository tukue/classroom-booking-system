package com.classroombooking.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTest {
    @Test
    void detectsOverlappingButAllowsAdjacentIntervals() {
        Booking booking = booking("09:00:00Z", "10:00:00Z");

        assertTrue(booking.overlaps(time("09:30:00Z"), time("10:30:00Z")));
        assertFalse(booking.overlaps(time("10:00:00Z"), time("11:00:00Z")));
        assertFalse(booking.overlaps(time("08:00:00Z"), time("09:00:00Z")));
    }

    @Test
    void rejectsInvalidTimeRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), time("10:00:00Z"), time("10:00:00Z"), null));
    }

    private Booking booking(String start, String end) {
        return new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), time(start), time(end), null);
    }

    private OffsetDateTime time(String time) {
        return OffsetDateTime.parse("2026-12-01T" + time);
    }
}
