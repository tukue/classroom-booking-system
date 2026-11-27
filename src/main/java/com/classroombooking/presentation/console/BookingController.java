package com.classroombooking.presentation.console;

import com.classroombooking.application.booking.BookingService;
import com.classroombooking.application.roomsearch.RoomRequirementsFactory;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.BookingRequest;
import com.classroombooking.domain.model.RoomRequirements;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

/** Converts console booking input into a validated request for the booking service. */
public final class BookingController {
    private final BookingService bookingService;
    private final RoomRequirementsFactory requirementsFactory;

    public BookingController(BookingService bookingService,
                             RoomRequirementsFactory requirementsFactory) {
        this.bookingService = Objects.requireNonNull(bookingService);
        this.requirementsFactory = Objects.requireNonNull(requirementsFactory);
    }

    public Booking createBooking(String customerId, String bookingUserName, String bookingUserEmail,
                                 String classroomId, String startsAt, String endsAt,
                                 int requiredCapacity, String equipmentNames,
                                 boolean accessibilityRequired, String comments) {
        UUID selectedCustomer = parseId(customerId, "customer");
        UUID selectedClassroom = parseId(classroomId, "classroom");
        RoomRequirements requirements = requirementsFactory.create(
                requiredCapacity, equipmentNames, accessibilityRequired);
        BookingRequest request = new BookingRequest(selectedCustomer, bookingUserName, bookingUserEmail,
                selectedClassroom, parseTime(startsAt), parseTime(endsAt), requirements, comments);
        return bookingService.createBooking(request);
    }

    private UUID parseId(String value, String label) {
        try {
            return UUID.fromString(value.strip());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Select a valid " + label + ".");
        }
    }

    private OffsetDateTime parseTime(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new IllegalArgumentException(
                    "Enter date and time in ISO-8601 format with a timezone offset.");
        }
    }
}
