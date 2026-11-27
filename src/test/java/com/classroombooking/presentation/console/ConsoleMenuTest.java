package com.classroombooking.presentation.console;

import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.application.classroom.ClassroomService;
import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.BookingAvailabilityRepository;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMenuTest {
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
        assertTrue(!persistenceMessage.contains("secret_schema"));
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
        assertTrue(!display.contains("Booked Room"));
    }

    @Test
    void stopsSearchWhenTimeInputEndsBeforeCapacity() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleMenu menu = menu("7\n2026-12-02T09:00:00Z\n", output,
                new InMemoryCustomerRepository());

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertTrue(!display.contains("Required capacity:"));
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
        return new ConsoleMenu(classroomController,
                new CustomerController(new CustomerService(customerRepository)), roomSearchController, view);
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
