package com.classroombooking.presentation.console;

import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.application.classroom.ClassroomService;
import com.classroombooking.application.booking.BookingService;
import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.application.roomsearch.RoomRequirementsFactory;
import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.BookingUser;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.model.IndividualCustomer;
import com.classroombooking.domain.repository.BookingAvailabilityRepository;
import com.classroombooking.domain.repository.BookingRepository;
import com.classroombooking.domain.repository.BookingUserRepository;
import com.classroombooking.domain.repository.ClassroomRepository;
import com.classroombooking.domain.repository.CustomerRepository;
import com.classroombooking.domain.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMenuTest {
    @Test
    void completesCustomerBookingFlowThroughConsoleMenu() {
        Equipment projector = new Equipment(UUID.randomUUID(), "PROJECTOR");
        Classroom classroom = new Classroom(UUID.randomUUID(), "Training Room", 20,
                java.util.Set.of(projector), true);
        InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
        Customer bookingCustomer = customers.save(new IndividualCustomer(UUID.randomUUID(),
                "Booking Customer", "booking.customer@example.test"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String input = "1\n2\nAlex Customer\nalex.customer@example.test\n"
                + "11\n" + bookingCustomer.id() + "\nTest User\ntest.user@example.test\n"
                + "2026-12-02T09:00:00Z\n2026-12-02T10:00:00Z\n12\nPROJECTOR\ny\n"
                + classroom.id() + "\nTeam workshop\n0\n";
        ConsoleMenu menu = menu(input, output, customers, List.of(classroom), List.of(), List.of(projector));

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertTrue(display.contains("Registered customer Alex Customer."));
        assertTrue(display.contains("Training Room | capacity: 20 | accessible: yes"));
        assertTrue(display.contains("Booking created:"));
        assertTrue(display.contains("Goodbye."));
        assertFalse(display.contains("Error:"));
        assertEquals(2, customers.findAll().size());
    }

    @Test
    void displaysEmptyAndCommentedBookingSchedules() {
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(outputBytes, true, StandardCharsets.UTF_8)) {
            ConsoleView view = new ConsoleView(new ByteArrayInputStream(new byte[0]), output);
            view.showBookings(List.of());
            view.showBookings(List.of(new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), OffsetDateTime.parse("2026-12-01T09:00:00Z"),
                    OffsetDateTime.parse("2026-12-01T10:00:00Z"), "Arrange projector")));
        }
        String display = outputBytes.toString(StandardCharsets.UTF_8);
        assertTrue(display.contains("No upcoming bookings found."));
        assertTrue(display.contains("Upcoming bookings (UTC)"));
        assertTrue(display.contains("Arrange projector"));
    }

    @Test
    void registersListsAndExitsFromTheConsoleMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
        ConsoleMenu menu = menu("2\n1\n1\nAcme Ltd\nsales@acme.test\n"
                + "1\n2\nAlex Customer\nalex.customer@example.test\n2\n0\n", output, repository);

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertTrue(display.contains("No customers registered."));
        assertTrue(display.contains("Registered customer Acme Ltd."));
        assertTrue(display.contains("Registered customer Alex Customer."));
        assertTrue(display.contains("sales@acme.test"));
        assertTrue(display.contains("alex.customer@example.test"));
        assertTrue(display.contains("Goodbye."));
    }

    @Test
    void reportsInvalidMenuAndCustomerTypeChoices() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleMenu menu = menu("not a number\n1\n8\n0\n", output,
                new InMemoryCustomerRepository());

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertTrue(display.contains("Choose one of the listed menu options."));
        assertTrue(display.contains("Choose company or individual."));
    }

    @Test
    void reportsValidationAndPersistenceFailures() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleMenu invalidInputMenu = menu("1\n1\n \ncompany@example.test\n0\n",
                output, new InMemoryCustomerRepository());
        invalidInputMenu.run();
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Company name must not be blank."));

        ByteArrayOutputStream persistenceOutput = new ByteArrayOutputStream();
        ConsoleMenu persistenceFailureMenu = menu("1\n2\nAlex Customer\nalex.customer@example.test\n0\n",
                persistenceOutput, new InMemoryCustomerRepository(true));
        persistenceFailureMenu.run();
        String persistenceMessage = persistenceOutput.toString(StandardCharsets.UTF_8);
        assertTrue(persistenceMessage.contains("Customer could not be saved. Please try again."));
        assertFalse(persistenceMessage.contains("secret_schema"));
    }

    @Test
    void searchesRoomsByRequirementsAndHidesRoomsWithOverlappingBookings() {
        Equipment projector = new Equipment(UUID.randomUUID(), "PROJECTOR");
        Classroom available = new Classroom(UUID.randomUUID(), "Available Room", 20,
                java.util.Set.of(projector), true);
        Classroom booked = new Classroom(UUID.randomUUID(), "Booked Room", 24,
                java.util.Set.of(projector), true);
        Booking existingBooking = new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                booked.id(), OffsetDateTime.parse("2026-12-02T09:00:00Z"),
                OffsetDateTime.parse("2026-12-02T10:00:00Z"), null);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleMenu menu = menu("7\n2026-12-02T09:30:00Z\n2026-12-02T10:30:00Z\n"
                        + "12\nPROJECTOR\ny\n0\n", output,
                new InMemoryCustomerRepository(), List.of(available, booked),
                List.of(existingBooking), List.of(projector));

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertTrue(display.contains("Available Room | capacity: 20 | accessible: yes"));
        assertFalse(display.contains("Booked Room"));
    }

    @Test
    void reportsAmbiguousEquipmentAndKeepsTheMenuRunning() {
        Equipment projectorUppercase = new Equipment(UUID.randomUUID(), "PROJECTOR");
        Equipment projectorLowercase = new Equipment(UUID.randomUUID(), "projector");
        Classroom room = new Classroom(UUID.randomUUID(), "Projector Room", 20,
                java.util.Set.of(projectorUppercase), true);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleMenu menu = menu("7\n2026-12-02T09:00:00Z\n2026-12-02T10:00:00Z\n"
                        + "10\nPROJECTOR\ny\n0\n", output, new InMemoryCustomerRepository(),
                List.of(room), List.of(), List.of(projectorUppercase, projectorLowercase));

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertTrue(display.contains("Equipment name is ambiguous: PROJECTOR"));
        assertTrue(display.contains("Goodbye."));
    }

    @Test
    void stopsSearchWhenTimeInputEndsBeforeCapacity() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleMenu menu = menu("7\n2026-12-02T09:00:00Z\n", output,
                new InMemoryCustomerRepository());

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertFalse(display.contains("Required capacity:"));
        assertTrue(display.contains("Goodbye."));
    }

    private ConsoleMenu menu(String input, ByteArrayOutputStream output,
                              InMemoryCustomerRepository repository) {
        return menu(input, output, repository, List.of(), List.of(),
                List.of(new Equipment(UUID.randomUUID(), "PROJECTOR")));
    }

    private ConsoleMenu menu(String input, ByteArrayOutputStream output,
                             InMemoryCustomerRepository customerRepository,
                             List<Classroom> classrooms, List<Booking> bookings,
                             List<Equipment> equipment) {
        ConsoleView view = new ConsoleView(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
        ClassroomRepository classroomRepository = new InMemoryClassroomRepository(classrooms);
        BookingAvailabilityRepository bookingRepository = (startsAt, endsAt) -> bookings.stream()
                .filter(booking -> booking.overlaps(startsAt, endsAt))
                .toList();
        EquipmentRepository equipmentRepository = () -> equipment;
        RoomSearchService roomSearchService = new RoomSearchService(classroomRepository, bookingRepository);
        RoomSearchController roomSearchController = new RoomSearchController(roomSearchService,
                equipmentRepository);
        ClassroomController classroomController = new ClassroomController(
                new ClassroomService(classroomRepository, equipmentRepository));
        InMemoryBookingUserRepository bookingUsers = new InMemoryBookingUserRepository();
        BookingService bookingService = new BookingService(new InMemoryBookingRepository(bookingUsers),
                bookingUsers, classroomRepository, customerRepository);
        BookingController bookingController = new BookingController(bookingService,
                new RoomRequirementsFactory(equipmentRepository));
        return new ConsoleMenu(classroomController,
                new CustomerController(new CustomerService(customerRepository)), roomSearchController,
                bookingController, view);
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        private final List<Customer> customers = new ArrayList<>();
        private final boolean failOnSave;

        private InMemoryCustomerRepository() {
            this(false);
        }

        private InMemoryCustomerRepository(boolean failOnSave) {
            this.failOnSave = failOnSave;
        }

        @Override
        public Customer save(Customer customer) {
            if (failOnSave) {
                throw new PersistenceException("Customer could not be saved. Please try again.",
                        new IllegalStateException("secret_schema.customers_email_key"));
            }
            customers.add(customer);
            return customer;
        }

        @Override
        public Optional<Customer> findById(UUID customerId) {
            return customers.stream().filter(customer -> customer.id().equals(customerId)).findFirst();
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
        public void deleteById(UUID customerId) {
            customers.removeIf(customer -> customer.id().equals(customerId));
        }
    }

    private static final class InMemoryBookingRepository implements BookingRepository {
        private final List<Booking> bookings = new ArrayList<>();
        private final InMemoryBookingUserRepository bookingUsers;

        private InMemoryBookingRepository(InMemoryBookingUserRepository bookingUsers) {
            this.bookingUsers = bookingUsers;
        }

        @Override
        public Booking save(Booking booking, BookingUser bookingUser) {
            if (!findOverlapping(booking.classroomId(), booking.startsAt(), booking.endsAt()).isEmpty()) {
                throw new IllegalArgumentException("The selected classroom is already booked for that time.");
            }
            BookingUser persistedUser = bookingUsers.findByEmail(bookingUser.email())
                    .orElseGet(() -> bookingUsers.save(bookingUser));
            Booking persisted = new Booking(booking.id(), booking.customerId(), persistedUser.id(),
                    booking.classroomId(), booking.startsAt(), booking.endsAt(), booking.comments());
            bookings.add(persisted);
            return persisted;
        }

        @Override
        public Optional<Booking> findById(UUID bookingId) {
            return bookings.stream().filter(booking -> booking.id().equals(bookingId)).findFirst();
        }

        @Override
        public List<Booking> findOverlapping(UUID classroomId, OffsetDateTime startsAt,
                                             OffsetDateTime endsAt) {
            return bookings.stream().filter(booking -> booking.classroomId().equals(classroomId)
                    && booking.overlaps(startsAt, endsAt)).toList();
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
    }

    private static final class InMemoryBookingUserRepository implements BookingUserRepository {
        private final List<BookingUser> bookingUsers = new ArrayList<>();

        @Override
        public BookingUser save(BookingUser bookingUser) {
            bookingUsers.add(bookingUser);
            return bookingUser;
        }

        @Override
        public Optional<BookingUser> findById(UUID bookingUserId) {
            return bookingUsers.stream().filter(user -> user.id().equals(bookingUserId)).findFirst();
        }

        @Override
        public Optional<BookingUser> findByEmail(String email) {
            return bookingUsers.stream().filter(user -> user.email().equals(email)).findFirst();
        }
    }

    private record InMemoryClassroomRepository(List<Classroom> classrooms)
            implements ClassroomRepository {
        @Override
        public Classroom save(Classroom classroom) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Classroom> findById(UUID classroomId) {
            return classrooms.stream().filter(room -> room.id().equals(classroomId)).findFirst();
        }

        @Override
        public List<Classroom> findAll() {
            return classrooms;
        }

        @Override
        public void update(Classroom classroom) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(UUID classroomId) {
            throw new UnsupportedOperationException();
        }
    }
}
