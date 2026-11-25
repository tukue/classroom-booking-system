package com.tukue.classroombooking.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingUserTest {
    @Test
    void storesBookingUserIdentityAndContactDetails() {
        UUID id = UUID.randomUUID();
        BookingUser user = new BookingUser(id, "Booking Operator", "operator@example.test");

        assertEquals(id, user.id());
        assertEquals("Booking Operator", user.name());
        assertEquals("operator@example.test", user.email());
    }
}
