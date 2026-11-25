package com.tukue.classroombooking.domain.model;

import java.util.UUID;

/** A company booking user who creates reservations on behalf of a customer. */
public record BookingUser(UUID id, String name, String email) {
}
