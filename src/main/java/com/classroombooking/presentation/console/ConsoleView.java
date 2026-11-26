package com.classroombooking.presentation.console;

import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

/** Handles console prompts and output without applying customer or persistence rules. */
public final class ConsoleView {
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleView(InputStream input, PrintStream output) {
        this.scanner = new Scanner(Objects.requireNonNull(input), StandardCharsets.UTF_8);
        this.output = Objects.requireNonNull(output);
    }

    public int readMenuChoice() {
        output.println("\nClassroom Booking System");
        output.println("1. Register customer");
        output.println("2. List customers");
        output.println("3. Search available classrooms");
        output.println("0. Exit");
        return readInteger("Choose an option: ");
    }

    public int readCustomerType() {
        output.println("1. Company");
        output.println("2. Individual");
        return readInteger("Customer type: ");
    }

    public int readInteger(String prompt) {
        return readIntegerValue(prompt);
    }

    public String readText(String prompt) {
        output.print(prompt);
        output.flush();
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    public void showCustomers(List<Customer> customers) {
        if (customers.isEmpty()) {
            output.println("No customers registered.");
            return;
        }
        customers.forEach(customer -> output.printf("%s | %s | %s%n",
                customer.id(), customer.name(), customer.email()));
    }

    public boolean readYesNo(String prompt) {
        String value = readText(prompt);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Enter yes or no.");
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "yes", "y" -> true;
            case "no", "n" -> false;
            default -> throw new IllegalArgumentException("Enter yes or no.");
        };
    }

    public void showEquipmentOptions(List<Equipment> equipment) {
        output.println("Available equipment: " + equipment.stream()
                .map(Equipment::name)
                .sorted()
                .collect(Collectors.joining(", ")));
    }

    public void showAvailableClassrooms(List<Classroom> classrooms) {
        if (classrooms.isEmpty()) {
            output.println("No available classrooms match those requirements.");
            return;
        }
        classrooms.forEach(classroom -> output.printf("%s | %s | capacity: %d | accessible: %s | equipment: %s%n",
                classroom.id(), classroom.name(), classroom.seatingCapacity(),
                classroom.accessible() ? "yes" : "no",
                classroom.equipment().stream().map(Equipment::name).sorted()
                        .collect(Collectors.joining(", "))));
    }

    public void showMessage(String message) {
        output.println(message);
    }

    public void showError(String message) {
        output.println("Error: " + message);
    }

    private int readIntegerValue(String prompt) {
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
