package com.classroombooking.application.booking;

import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.BookingRequest;
import com.classroombooking.domain.model.BookingUser;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.CompanyCustomer;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.model.RoomRequirements;
import com.classroombooking.domain.repository.BookingRepository;
import com.classroombooking.domain.repository.BookingUserRepository;
import com.classroombooking.domain.repository.ClassroomRepository;
import com.classroombooking.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingServiceTest {
    @Test
    void createsBookingLinkedToCustomerBookingUserAndRoom() {
        Fixture fixture = new Fixture(true);

        Booking booking = fixture.service.createBooking(fixture.request(
                "09:00:00Z", "10:00:00Z", "Allergy note"));

        assertEquals(fixture.customer.id(), booking.customerId());
        assertEquals(fixture.room.id(), booking.classroomId());
        assertEquals(fixture.bookingUsers.findByEmail("operator@example.test").orElseThrow().id(),
                booking.bookingUserId());
        assertEquals("Allergy note", booking.comments());
        assertEquals(1, fixture.bookings.findAll().size());
    }

    @Test
    void rejectsConflictingTimesAndRoomRequirementMismatches() {
        Fixture fixture = new Fixture(true);
        fixture.service.createBooking(fixture.request(
                "09:00:00Z", "10:00:00Z", null));

        assertThrows(BookingDuplicateException.class,
                () -> fixture.service.createBooking(fixture.request(
                        "09:30:00Z", "10:30:00Z", null)));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.createBooking(new BookingRequest(
                        fixture.customer.id(), "Operator", "operator@example.test", fixture.room.id(),
                        time("11:00:00Z"), time("12:00:00Z"),
                        new RoomRequirements(20, Set.of(fixture.projector, fixture.whiteboard), true), null)));
        assertEquals(1, fixture.bookings.findAll().size());
        assertEquals(1, fixture.bookingUsers.findAll().size());
    }

    @Test
    void reusesBookingUserByEmailAndAllowsAnAdjacentReservation() {
        Fixture fixture = new Fixture(true);
        Booking first = fixture.service.createBooking(fixture.request(
                "09:00:00Z", "10:00:00Z", null));
        Booking adjacent = fixture.service.createBooking(fixture.request(
                "10:00:00Z", "11:00:00Z", null));

        assertEquals(first.bookingUserId(), adjacent.bookingUserId());
        assertEquals(1, fixture.bookingUsers.findAll().size());
        assertEquals(2, fixture.bookings.findAll().size());
    }

    private static OffsetDateTime time(String value) {
        return OffsetDateTime.parse("2026-12-03T" + value);
    }

    private static final class Fixture {
        private final Equipment projector = new Equipment(UUID.randomUUID(), "PROJECTOR");
        private final Equipment whiteboard = new Equipment(UUID.randomUUID(), "WHITEBOARD");
        private final Customer customer = new CompanyCustomer(UUID.randomUUID(), "Example Company",
                "customer@example.test");
        private final Classroom room;
        private final MemoryCustomers customers = new MemoryCustomers();
        private final MemoryClassrooms classrooms = new MemoryClassrooms();
        private final MemoryBookingUsers bookingUsers = new MemoryBookingUsers();
        private final MemoryBookings bookings = new MemoryBookings(bookingUsers);
        private final BookingService service;

        private Fixture(boolean accessible) {
            room = new Classroom(UUID.randomUUID(), "Training Room", 24,
                    Set.of(projector), accessible);
            customers.save(customer);
            classrooms.save(room);
            service = new BookingService(bookings, bookingUsers, classrooms, customers);
        }

        private BookingRequest request(String startsAt, String endsAt, String comments) {
            return new BookingRequest(customer.id(), "Booking Operator", "operator@example.test",
                    room.id(), time(startsAt), time(endsAt),
                    new RoomRequirements(20, Set.of(projector), true), comments);
        }
    }

    private static final class MemoryCustomers implements CustomerRepository {
        private final List<Customer> customers = new ArrayList<>();

        @Override
        public Customer save(Customer customer) {
            customers.add(customer);
            return customer;
        }

        @Override
        public Optional<Customer> findById(UUID id) {
            return customers.stream().filter(customer -> customer.id().equals(id)).findFirst();
        }

        @Override
        public List<Customer> findAll() {
            return List.copyOf(customers);
        }

        @Override
        public void update(Customer customer) {
            deleteById(customer.id());
            customers.add(customer);
        }

        @Override
        public void deleteById(UUID id) {
            customers.removeIf(customer -> customer.id().equals(id));
        }
    }

    private static final class MemoryClassrooms implements ClassroomRepository {
        private final List<Classroom> classrooms = new ArrayList<>();

        @Override
        public Classroom save(Classroom classroom) {
            classrooms.add(classroom);
            return classroom;
        }

        @Override
        public Optional<Classroom> findById(UUID id) {
            return classrooms.stream().filter(classroom -> classroom.id().equals(id)).findFirst();
        }

        @Override
        public List<Classroom> findAll() {
            return List.copyOf(classrooms);
        }

        @Override
        public void update(Classroom classroom) {
            deleteById(classroom.id());
            classrooms.add(classroom);
        }

        @Override
        public void deleteById(UUID id) {
            classrooms.removeIf(classroom -> classroom.id().equals(id));
        }
    }

    private static final class MemoryBookingUsers implements BookingUserRepository {
        private final List<BookingUser> users = new ArrayList<>();

        @Override
        public BookingUser save(BookingUser user) {
            users.add(user);
            return user;
        }

        @Override
        public Optional<BookingUser> findById(UUID id) {
            return users.stream().filter(user -> user.id().equals(id)).findFirst();
        }

        @Override
        public Optional<BookingUser> findByEmail(String email) {
            return users.stream().filter(user -> user.email().equals(email)).findFirst();
        }

        private List<BookingUser> findAll() {
            return List.copyOf(users);
        }
    }

    private static final class MemoryBookings implements BookingRepository {
        private final List<Booking> bookings = new ArrayList<>();
        private final MemoryBookingUsers bookingUsers;

        private MemoryBookings(MemoryBookingUsers bookingUsers) {
            this.bookingUsers = bookingUsers;
        }

        @Override
        public Booking save(Booking booking, BookingUser bookingUser) {
            if (!findOverlapping(booking.classroomId(), booking.startsAt(), booking.endsAt()).isEmpty()) {
                throw new BookingDuplicateException("Room is already booked.");
            }
            BookingUser persistedUser = bookingUsers.findByEmail(bookingUser.email())
                    .orElseGet(() -> bookingUsers.save(bookingUser));
            Booking persisted = new Booking(booking.id(), booking.customerId(), persistedUser.id(),
                    booking.classroomId(), booking.startsAt(), booking.endsAt(), booking.comments());
            bookings.add(persisted);
            return persisted;
        }

        @Override
        public Optional<Booking> findById(UUID id) {
            return bookings.stream().filter(booking -> booking.id().equals(id)).findFirst();
        }

        @Override
        public List<Booking> findOverlapping(UUID classroomId, OffsetDateTime start, OffsetDateTime end) {
            return bookings.stream().filter(booking -> booking.classroomId().equals(classroomId)
                    && booking.overlaps(start, end)).toList();
        }

        @Override
        public List<Booking> findUpcomingByClassroom(UUID classroomId, OffsetDateTime from) {
            return bookings.stream().filter(booking -> booking.classroomId().equals(classroomId)
                    && !booking.startsAt().isBefore(from)).toList();
        }

        @Override
        public List<Booking> findUpcomingByCustomer(UUID customerId, OffsetDateTime from) {
            return bookings.stream().filter(booking -> booking.customerId().equals(customerId)
                    && !booking.startsAt().isBefore(from)).toList();
        }

        @Override
        public List<Booking> findAllUpcoming(OffsetDateTime from) {
            return bookings.stream().filter(booking -> !booking.startsAt().isBefore(from)).toList();
        }

        private List<Booking> findAll() {
            return List.copyOf(bookings);
        }
    }
}
