package com.classroombooking.presentation.console;

import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.Equipment;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;

/** Handles console prompts and output without applying domain or persistence rules. */
public final class ConsoleView {
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleView(InputStream input, PrintStream output) {
        this.scanner = new Scanner(Objects.requireNonNull(input), StandardCharsets.UTF_8);
        this.output = Objects.requireNonNull(output);
    }

    public int readMenuChoice() {
        return readMenuChoice(true, true, false);
    }

    int readMenuChoice(boolean classroomsEnabled, boolean roomSearchEnabled, boolean bookingSchedulesEnabled) {
        output.println("\nClassroom Booking System");
        output.println("1. Register customer");
        output.println("2. List customers");
        if (classroomsEnabled) {
            output.println("3. List classrooms");
            output.println("4. Create classroom");
            output.println("5. Update classroom");
            output.println("6. Delete classroom");
        }
        if (roomSearchEnabled) {
            output.println("7. Search available classrooms");
        }
        if (classroomsEnabled && bookingSchedulesEnabled) {
            output.println("8. View upcoming bookings by classroom");
        }
        if (bookingSchedulesEnabled) {
            output.println("9. View upcoming bookings by customer");
            output.println("10. View all upcoming bookings");
        }
        output.println("0. Exit");
        return readInteger("Choose an option: ");
    }

    public int readCustomerType() {
        output.println("1. Company");
        output.println("2. Individual");
        return readInteger("Customer type: ");
    }

    public int readInteger(String prompt) {
        String value = readText(prompt);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    public String readText(String prompt) {
        output.print(prompt);
        output.flush();
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    UUID readClassroomId() {
        return readId("Classroom", "classroom");
    }

    UUID readCustomerId() {
        return readId("Customer", "customer");
    }

    private UUID readId(String label, String entity) {
        String id = readText(label + " ID: ");
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id.strip());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Enter a valid " + entity + " ID.", exception);
        }
    }

    ClassroomDetails readClassroomDetails(List<Equipment> equipmentCatalog) {
        String name = readText("Classroom name: ");
        String capacity = readText("Seating capacity: ");
        String equipment = readText("Equipment (comma-separated, blank for none). Available: "
                + equipmentNames(equipmentCatalog) + ". Enter equipment: ");
        String accessibility = readText("Accessible? (yes/no): ");
        if (name == null || capacity == null || equipment == null || accessibility == null) {
            return null;
        }
        int seatingCapacity;
        try {
            seatingCapacity = Integer.parseInt(capacity.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Seating capacity must be a whole number.", exception);
        }
        List<String> selectedEquipment = equipment.isBlank()
                ? List.of() : List.of(equipment.split(",", -1));
        return new ClassroomDetails(name, seatingCapacity, selectedEquipment, parseYesNo(accessibility));
    }

    boolean readYesNo(String prompt) {
        return parseYesNo(readText(prompt));
    }

    private boolean parseYesNo(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enter yes or no for accessibility.");
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "yes", "y" -> true;
            case "no", "n" -> false;
            default -> throw new IllegalArgumentException("Enter yes or no for accessibility.");
        };
    }

    void showBookings(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            showMessage("No upcoming bookings found.");
            return;
        }
        showMessage("Upcoming bookings (UTC)");
        for (Booking booking : bookings) {
            output.printf("%s to %s | Classroom: %s | Customer: %s | Booking: %s%n",
                    booking.startsAt(), booking.endsAt(), booking.classroomId(), booking.customerId(), booking.id());
            if (booking.comments() != null && !booking.comments().isBlank()) {
                output.println(booking.comments());
            }
        }
    }

    void showClassrooms(List<Classroom> classrooms) {
        if (classrooms.isEmpty()) {
            output.println("No classrooms found.");
            return;
        }
        for (Classroom room : classrooms) {
            output.printf("%s | %s | %d seats | Accessible: %s | Equipment: %s%n",
                    room.id(), room.name(), room.seatingCapacity(), room.accessible() ? "yes" : "no",
                    equipmentNames(room.equipment()));
        }
    }

    public void showCustomers(List<Customer> customers) {
        if (customers.isEmpty()) {
            output.println("No customers registered.");
            return;
        }
        customers.forEach(customer -> output.printf("%s | %s | %s%n",
                customer.id(), customer.name(), customer.email()));
    }

    void showEquipmentOptions(List<Equipment> equipment) {
        output.println("Available equipment: " + equipmentNames(equipment));
    }

    void showAvailableClassrooms(List<Classroom> classrooms) {
        if (classrooms.isEmpty()) {
            output.println("No available classrooms match those requirements.");
            return;
        }
        classrooms.forEach(classroom -> output.printf(
                "%s | %s | capacity: %d | accessible: %s | equipment: %s%n",
                classroom.id(), classroom.name(), classroom.seatingCapacity(),
                classroom.accessible() ? "yes" : "no", equipmentNames(classroom.equipment())));
    }

    public void showMessage(String message) {
        output.println(message);
    }

    public void showError(String message) {
        output.println("Error: " + message);
    }

    private String equipmentNames(Collection<Equipment> equipment) {
        return equipment.isEmpty() ? "none" : equipment.stream().map(Equipment::name)
                .sorted().collect(Collectors.joining(", "));
    }

    record ClassroomDetails(String name, int capacity, List<String> equipment, boolean accessible) {
    }
}
