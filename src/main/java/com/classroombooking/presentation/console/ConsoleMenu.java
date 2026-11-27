package com.classroombooking.presentation.console;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Customer;

import java.util.Objects;
import java.util.UUID;

/** Runs the customer, classroom, and room-search console actions. */
public final class ConsoleMenu {
    private final ClassroomController classroomController;
    private final CustomerController customerController;
    private final RoomSearchController roomSearchController;
    private final BookingController bookingController;
    private final ConsoleView view;

    public ConsoleMenu(ClassroomController classroomController, CustomerController customerController,
                       RoomSearchController roomSearchController, BookingController bookingController,
                       ConsoleView view) {
        this.classroomController = Objects.requireNonNull(classroomController);
        this.customerController = Objects.requireNonNull(customerController);
        this.roomSearchController = Objects.requireNonNull(roomSearchController);
        this.bookingController = Objects.requireNonNull(bookingController);
        this.view = Objects.requireNonNull(view);
    }

    public void run() {
        while (true) {
            int choice = view.readMenuChoice();
            try {
                switch (choice) {
                    case 0 -> {
                        view.showMessage("Goodbye.");
                        return;
                    }
                    case 1 -> registerCustomer();
                    case 2 -> view.showCustomers(customerController.findAllCustomers());
                    case 3 -> view.showClassrooms(classroomController.listClassrooms());
                    case 4 -> saveClassroom(null);
                    case 5 -> updateClassroom();
                    case 6 -> deleteClassroom();
                    case 7 -> searchAvailableClassrooms();
                    case 8 -> createBooking();
                    default -> view.showError("Choose one of the listed menu options.");
                }
            } catch (IllegalArgumentException | PersistenceException exception) {
                view.showError(exception.getMessage());
            }
        }
    }

    private void registerCustomer() {
        int customerType = view.readCustomerType();
        if (customerType == 0) {
            return;
        }
        if (customerType != 1 && customerType != 2) {
            view.showError("Choose company or individual.");
            return;
        }
        String name = view.readText("Customer name: ");
        String email = view.readText("Customer email: ");
        if (name == null || email == null) {
            return;
        }

        Customer customer = customerType == 1
                ? customerController.registerCompany(name, email)
                : customerController.registerIndividual(name, email);
        view.showMessage("Registered customer " + customer.name() + ".");
    }

    private void saveClassroom(UUID id) {
        ConsoleView.ClassroomDetails details = view.readClassroomDetails(
                classroomController.listEquipment());
        if (details == null) {
            return;
        }
        if (id == null) {
            Classroom created = classroomController.create(details.name(), details.capacity(),
                    details.equipment(), details.accessible());
            view.showMessage("Created classroom " + created.name() + " (" + created.id() + ").");
        } else {
            classroomController.update(id, details.name(), details.capacity(),
                    details.equipment(), details.accessible());
            view.showMessage("Classroom updated.");
        }
    }

    private void updateClassroom() {
        UUID classroomId = view.readClassroomId();
        if (classroomId != null) {
            saveClassroom(classroomId);
        }
    }

    private void deleteClassroom() {
        UUID classroomId = view.readClassroomId();
        if (classroomId != null) {
            classroomController.delete(classroomId);
            view.showMessage("Classroom deleted.");
        }
    }

    private void searchAvailableClassrooms() {
        String startsAt = view.readText("Start time (ISO-8601 with offset): ");
        String endsAt = view.readText("End time (ISO-8601 with offset): ");
        if (startsAt == null || endsAt == null) {
            return;
        }
        int capacity = view.readInteger("Required capacity: ");
        view.showEquipmentOptions(roomSearchController.findAllEquipment());
        String requiredEquipment = view.readText("Required equipment (comma-separated, optional): ");
        if (requiredEquipment == null) {
            return;
        }
        boolean accessibilityRequired = view.readYesNo("Accessibility required? (yes/no): ");
        view.showAvailableClassrooms(roomSearchController.findAvailableRooms(
                startsAt, endsAt, capacity, requiredEquipment, accessibilityRequired));
    }

    private void createBooking() {
        var customers = customerController.findAllCustomers();
        view.showCustomers(customers);
        if (customers.isEmpty()) {
            view.showError("Register a customer before creating a booking.");
            return;
        }

        String customerId = view.readText("Customer ID: ");
        String bookingUserName = view.readText("Booking user name: ");
        String bookingUserEmail = view.readText("Booking user email: ");
        String startsAt = view.readText("Start time (ISO-8601 with offset): ");
        String endsAt = view.readText("End time (ISO-8601 with offset): ");
        if (customerId == null || bookingUserName == null || bookingUserEmail == null
                || startsAt == null || endsAt == null) {
            return;
        }

        int capacity = view.readInteger("Required capacity: ");
        view.showEquipmentOptions(roomSearchController.findAllEquipment());
        String requiredEquipment = view.readText("Required equipment (comma-separated, optional): ");
        if (requiredEquipment == null) {
            return;
        }
        boolean accessibilityRequired = view.readYesNo("Accessibility required? (yes/no): ");
        var availableRooms = roomSearchController.findAvailableRooms(startsAt, endsAt,
                capacity, requiredEquipment, accessibilityRequired);
        view.showAvailableClassrooms(availableRooms);
        if (availableRooms.isEmpty()) {
            return;
        }

        String classroomId = view.readText("Classroom ID to book: ");
        if (classroomId == null) {
            return;
        }
        String comments = view.readText("Optional comments (press Enter for none): ");
        var booking = bookingController.createBooking(customerId, bookingUserName, bookingUserEmail,
                classroomId, startsAt, endsAt, capacity, requiredEquipment, accessibilityRequired, comments);
        view.showMessage("Booking created: " + booking.id() + ".");
    }

}
