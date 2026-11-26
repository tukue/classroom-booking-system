package com.classroombooking.presentation.console;

import com.classroombooking.domain.model.Customer;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        output.println("\nClassroom Booking System");
        output.println("1. Register customer");
        output.println("2. List customers");
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
