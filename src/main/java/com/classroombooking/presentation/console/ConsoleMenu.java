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
    private final ConsoleView view;

    public ConsoleMenu(CustomerController customerController, RoomSearchController roomSearchController,
                       ConsoleView view) {
        this(null, customerController, roomSearchController, view);
    }

    public ConsoleMenu(ClassroomController classroomController, CustomerController customerController,
                       ConsoleView view) {
        this(classroomController, customerController, null, view);
    }

    public ConsoleMenu(ClassroomController classroomController, CustomerController customerController,
                       RoomSearchController roomSearchController, ConsoleView view) {
        this.classroomController = classroomController;
        this.customerController = Objects.requireNonNull(customerController);
        this.roomSearchController = roomSearchController;
        this.view = Objects.requireNonNull(view);
    }

    public void run() {
        while (true) {
            int choice = view.readMenuChoice(classroomController != null,
                    roomSearchController != null);
            try {
                switch (choice) {
                    case 0 -> {
                        view.showMessage("Goodbye.");
                        return;
                    }
                    case 1 -> registerCustomer();
                    case 2 -> view.showCustomers(customerController.findAllCustomers());
                    case 3 -> {
                        if (classroomController == null) {
                            if (roomSearchController == null) {
                                invalidChoice();
                            } else {
                                searchAvailableClassrooms();
                            }
                        } else {
                            view.showClassrooms(classroomController.listClassrooms());
                        }
                    }
                    case 4 -> {
                        if (classroomController == null) {
                            invalidChoice();
                        } else {
                            saveClassroom(null);
                        }
                    }
                    case 5 -> {
                        if (classroomController == null) {
                            invalidChoice();
                        } else {
                            updateClassroom();
                        }
                    }
                    case 6 -> {
                        if (classroomController == null) {
                            invalidChoice();
                        } else {
                            deleteClassroom();
                        }
                    }
                    case 7 -> {
                        if (roomSearchController == null) {
                            invalidChoice();
                        } else {
                            searchAvailableClassrooms();
                        }
                    }
                    default -> invalidChoice();
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
        int capacity = view.readInteger("Required capacity: ");
        if (startsAt == null || endsAt == null) {
            return;
        }
        view.showEquipmentOptions(roomSearchController.findAllEquipment());
        String requiredEquipment = view.readText("Required equipment (comma-separated, optional): ");
        if (requiredEquipment == null) {
            return;
        }
        boolean accessibilityRequired = view.readYesNo("Accessibility required? (yes/no): ");
        view.showAvailableClassrooms(roomSearchController.findAvailableRooms(
                startsAt, endsAt, capacity, requiredEquipment, accessibilityRequired));
    }

    private void invalidChoice() {
        view.showError("Choose one of the listed menu options.");
    }
}
