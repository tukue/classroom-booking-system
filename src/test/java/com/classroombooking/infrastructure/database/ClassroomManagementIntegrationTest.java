package com.classroombooking.infrastructure.database;

import com.classroombooking.application.classroom.ClassroomService;
import com.classroombooking.application.booking.BookingService;
import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.application.roomsearch.RoomRequirementsFactory;
import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.infrastructure.persistence.JdbcClassroomRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingAvailabilityRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingUserRepository;
import com.classroombooking.infrastructure.persistence.JdbcBookingAvailabilityRepository;
import com.classroombooking.infrastructure.persistence.JdbcCustomerRepository;
import com.classroombooking.infrastructure.persistence.JdbcEquipmentRepository;
import com.classroombooking.presentation.console.ClassroomController;
import com.classroombooking.presentation.console.BookingController;
import com.classroombooking.presentation.console.ConsoleMenu;
import com.classroombooking.presentation.console.ConsoleView;
import com.classroombooking.presentation.console.CustomerController;
import com.classroombooking.presentation.console.RoomSearchController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "db.integration", matches = "true")
class ClassroomManagementIntegrationTest {
    private final DatabaseManager database = DatabaseManager.getInstance();
    private final JdbcClassroomRepository rooms = new JdbcClassroomRepository(database);
    private final DatabaseInitializer initializer = new DatabaseInitializer(database);

    @Test
    void consoleListsAndManagesTheInventoryAcrossRestarts() throws Exception {
        initializer.initialize();
        Classroom edited = rooms.findAll().getFirst();
        Classroom deleted = rooms.findAll().get(1);
        String name = "Classroom test " + UUID.randomUUID();
        String inventory = runConsole("3\n0\n");
        for (Classroom room : rooms.findAll()) {
            assertTrue(inventory.contains(room.id().toString()));
        }
        assertEquals(20, rooms.findAll().size());

        try {
            String output = runConsole("""
                    4
                    %s
                    18
                    whiteboard, PROJECTOR
                    yes
                    5
                    %s
                    %s
                    42

                    no
                    6
                    %s
                    0
                    """.formatted(name, edited.id(), edited.name(), deleted.id()));
            assertTrue(output.contains("Created classroom " + name));
            assertTrue(output.contains("Classroom updated."));
            assertTrue(output.contains("Classroom deleted."));

            initializer.initialize();
            var reloaded = new JdbcClassroomRepository(database);
            Classroom created = reloaded.findAll().stream().filter(room -> room.name().equals(name))
                    .findFirst().orElseThrow();
            assertEquals(18, created.seatingCapacity());
            assertTrue(created.accessible());
            assertEquals(Set.of("WHITEBOARD", "PROJECTOR"),
                    created.equipment().stream().map(Equipment::name).collect(Collectors.toSet()));
            assertEquals(new Classroom(edited.id(), edited.name(), 42, Set.of(), false),
                    reloaded.findById(edited.id()).orElseThrow());
            assertTrue(reloaded.findById(deleted.id()).isEmpty());
        } finally {
            rooms.update(edited);
            if (rooms.findById(deleted.id()).isEmpty()) {
                rooms.save(deleted);
            }
            rooms.findAll().stream().filter(room -> room.name().equals(name))
                    .forEach(room -> rooms.deleteById(room.id()));
        }
    }

    @Test
    void equipmentFailureRollsBackRoomChangesAndConsoleReportsInvalidInput() throws Exception {
        initializer.initialize();
        Classroom original = rooms.findAll().getFirst();
        Classroom invalid = new Classroom(original.id(), original.name(), original.seatingCapacity() + 1,
                Set.of(new Equipment(UUID.randomUUID(), "Missing")), !original.accessible());

        assertThrows(IllegalArgumentException.class, () -> rooms.update(invalid));
        assertEquals(original, rooms.findById(original.id()).orElseThrow());

        String output = runConsole("""
                4
                Invalid capacity
                ten

                no
                4
                Invalid equipment
                10
                Missing
                no
                4
                %s
                10

                no
                4

                10

                yes
                4
                %s
                10

                maybe
                6
                invalid-id
                6
                %s
                9
                0
                """.formatted(original.name(), original.name(), UUID.randomUUID()));
        assertTrue(output.contains("Seating capacity must be a whole number."));
        assertTrue(output.contains("Unknown equipment"));
        assertTrue(output.contains("A classroom with that name already exists."));
        assertTrue(output.contains("Classroom name must not be blank."));
        assertTrue(output.contains("Enter yes or no for accessibility."));
        assertTrue(output.contains("Enter a valid classroom ID."));
        assertTrue(output.contains("Classroom not found."));
        assertTrue(output.contains("Choose one of the listed menu options."));
    }

    private String runConsole(String actions) {
        var input = new ByteArrayInputStream(actions.getBytes(StandardCharsets.UTF_8));
        var output = new ByteArrayOutputStream();
        var view = new ConsoleView(input, new PrintStream(output, true, StandardCharsets.UTF_8));
        var classrooms = new ClassroomController(new ClassroomService(rooms, new JdbcEquipmentRepository(database)));
        var customers = new CustomerController(new CustomerService(new JdbcCustomerRepository(database)));
        var equipment = new JdbcEquipmentRepository(database);
        var requirements = new RoomRequirementsFactory(equipment);
        var searchService = new RoomSearchService(rooms, new JdbcBookingAvailabilityRepository(database));
        var search = new RoomSearchController(searchService, requirements);
        var bookingService = new BookingService(new JdbcBookingRepository(database),
                new JdbcBookingUserRepository(database), rooms, new JdbcCustomerRepository(database));
        var bookings = new BookingController(bookingService, requirements);
        new ConsoleMenu(classrooms, customers, search, bookings, view).run();
        return output.toString(StandardCharsets.UTF_8);
    }
}
