package com.tukue.classroombooking.domain.repository;

import com.tukue.classroombooking.domain.model.BookingUser;

import java.util.Optional;
import java.util.UUID;

/** Persistence port for users who create reservations on behalf of customers. */
public interface BookingUserRepository {
    BookingUser save(BookingUser bookingUser);

    Optional<BookingUser> findById(UUID bookingUserId);
}
