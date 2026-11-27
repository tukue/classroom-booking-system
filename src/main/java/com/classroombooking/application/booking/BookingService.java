package com.classroombooking.application.booking;

import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.BookingRequest;
import com.classroombooking.domain.model.BookingUser;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.repository.BookingRepository;
import com.classroombooking.domain.repository.BookingUserRepository;
import com.classroombooking.domain.repository.ClassroomRepository;
import com.classroombooking.domain.repository.CustomerRepository;

import java.util.Objects;
import java.util.UUID;

/** Coordinates booking validation, identity links, conflict checks, and persistence. */
public final class BookingService {
    private final BookingRepository bookingRepository;
    private final BookingUserRepository bookingUserRepository;
    private final ClassroomRepository classroomRepository;
    private final CustomerRepository customerRepository;

    public BookingService(BookingRepository bookingRepository,
                          BookingUserRepository bookingUserRepository,
                          ClassroomRepository classroomRepository,
                          CustomerRepository customerRepository) {
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
        this.bookingUserRepository = Objects.requireNonNull(bookingUserRepository);
        this.classroomRepository = Objects.requireNonNull(classroomRepository);
        this.customerRepository = Objects.requireNonNull(customerRepository);
    }

    public Booking createBooking(BookingRequest request) {
        Objects.requireNonNull(request, "request");
        var customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Select a registered customer."));
        Classroom classroom = classroomRepository.findById(request.classroomId())
                .orElseThrow(() -> new IllegalArgumentException("Select an existing classroom."));
        if (!classroom.meetsRequirements(request.requirements())) {
            throw new IllegalArgumentException("The selected classroom does not meet the requirements.");
        }
        if (!bookingRepository.findOverlapping(classroom.id(), request.startsAt(), request.endsAt()).isEmpty()) {
            throw new BookingConflictException("The selected classroom is already booked for that time.");
        }

        BookingUser bookingUser = bookingUserRepository.findByEmail(request.bookingUserEmail())
                .orElseGet(() -> new BookingUser(
                        UUID.randomUUID(), request.bookingUserName(), request.bookingUserEmail()));
        Booking booking = new Booking(UUID.randomUUID(), customer.id(), bookingUser.id(), classroom.id(),
                request.startsAt(), request.endsAt(), request.comments());
        return bookingRepository.save(booking, bookingUser);
    }
}
