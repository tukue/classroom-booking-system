package com.classroombooking.presentation.console;

import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.Equipment;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.Scanner;

/** Handles console prompts and output without applying customer or persistence rules. */
public final class ConsoleView {
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleView(InputStream input, PrintStream output) {
        this.scanner = new Scanner(Objects.requireNonNull(input), StandardCharsets.UTF_8);
        this.output = Objects.requireNonNull(output);
    }

    public int readMenuChoice() {
        return readMenuChoice(false);
    }

    public int readMenuChoice(boolean classroomsEnabled) {
        output.println("\nClassroom Booking System");
        output.println("1. Register customer");
        output.println("2. List customers");
        if (classroomsEnabled) {
            output.println("3. List classrooms");
            output.println("4. Create classroom");
            output.println("5. Update classroom");
            output.println("6. Delete classroom");
        }
        output.println("0. Exit");
        return readInteger("Choose an option: ");
    }

    public int readCustomerType() {
        output.println("1. Company");
        output.println("2. Individual");
        return readInteger("Customer type: ");
    }

    public String readText(String prompt) {
        output.print(prompt);
        output.flush();
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    UUID readClassroomId() {
        String id = readText("Classroom ID: ");
        if (id == null) {
            return null;
        }
        try {
            return UUID.fromString(id.strip());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Enter a valid classroom ID.", exception);
        }
    }

    ClassroomDetails readClassroomDetails(List<Equipment> catalog) {
        String name = readText("Classroom name: ");
        String capacity = readText("Seating capacity: ");
        String equipment = readText("Equipment (comma-separated, blank for none). Available: "
                + equipmentNames(catalog) + ". Enter equipment: ");
        String accessibility = readText("Accessible? (yes/no): ");
        if (name == null || capacity == null || equipment == null || accessibility == null) {
            return null;
        }
        int seats;
        try {
            seats = Integer.parseInt(capacity.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Seating capacity must be a whole number.", exception);
        }
        List<String> selected = equipment.isBlank() ? List.of() : List.of(equipment.split(",", -1));
        boolean accessible = switch (accessibility.strip().toLowerCase(Locale.ROOT)) {
            case "yes", "y" -> true;
            case "no", "n" -> false;
            default -> throw new IllegalArgumentException("Enter yes or no for accessibility.");
        };
        return new ClassroomDetails(name, seats, selected, accessible);
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

    private String equipmentNames(Collection<Equipment> equipment) {
        return equipment.isEmpty() ? "none" : equipment.stream()
                .map(Equipment::name).sorted().collect(Collectors.joining(", "));
    }

    record ClassroomDetails(String name, int capacity, List<String> equipment, boolean accessible) {
    }

    public void showCustomers(List<Customer> customers) {
        if (customers.isEmpty()) {
            output.println("No customers registered.");
            return;
        }
        customers.forEach(customer -> output.printf("%s | %s | %s%n",
                customer.id(), customer.name(), customer.email()));
    }

    public void showMessage(String message) {
        output.println(message);
    }

    public void showError(String message) {
        output.println("Error: " + message);
    }

    private int readInteger(String prompt) {
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
}
