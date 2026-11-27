package com.classroombooking.presentation.console;

import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.repository.BookingScheduleRepository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Coordinates read-only booking schedule requests from the console. */
public final class BookingScheduleController {
    private final BookingScheduleRepository bookings;
    private final Clock clock;

    public BookingScheduleController(BookingScheduleRepository bookings, Clock clock) {
        this.bookings = bookings;
        this.clock = clock;
    }

    public List<Booking> upcomingByClassroom(UUID classroomId) {
        return bookings.findUpcomingByClassroom(classroomId, OffsetDateTime.now(clock));
    }

    public List<Booking> upcomingByCustomer(UUID customerId) {
        return bookings.findUpcomingByCustomer(customerId, OffsetDateTime.now(clock));
    }
}
