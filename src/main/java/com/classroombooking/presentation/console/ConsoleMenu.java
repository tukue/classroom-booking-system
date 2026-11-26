package com.classroombooking.presentation.console;

import com.classroombooking.domain.model.Customer;
import com.classroombooking.application.persistence.PersistenceException;

import java.util.Objects;
import java.util.UUID;

/** Runs the customer and classroom console actions. */
public final class ConsoleMenu {
    private final CustomerController customerController;
    private final ClassroomController classroomController;
    private final ConsoleView view;

    public ConsoleMenu(CustomerController customerController, ConsoleView view) {
        this(null, customerController, view);
    }

    public ConsoleMenu(ClassroomController classroomController, CustomerController customerController,
                       ConsoleView view) {
        this.classroomController = classroomController;
        this.customerController = Objects.requireNonNull(customerController);
        this.view = Objects.requireNonNull(view);
    }

    public void run() {
        while (true) {
            int choice = view.readMenuChoice(classroomController != null);
            try {
                switch (choice) {
                    case 0 -> {
                        view.showMessage("Goodbye.");
                        return;
                    }
                    case 1 -> registerCustomer();
                    case 2 -> view.showCustomers(customerController.findAllCustomers());
                    default -> {
                        if (classroomController == null) {
                            view.showError("Choose one of the listed menu options.");
                        } else {
                            manageClassrooms(choice);
                        }
                    }
                }
            } catch (IllegalArgumentException | PersistenceException exception) {
                view.showError(exception.getMessage());
            }
        }
    }

    private void manageClassrooms(int choice) {
        switch (choice) {
            case 3 -> view.showClassrooms(classroomController.listClassrooms());
            case 4 -> saveClassroom(null);
            case 5 -> {
                UUID id = view.readClassroomId();
                if (id != null) {
                    saveClassroom(id);
                }
            }
            case 6 -> {
                UUID id = view.readClassroomId();
                if (id != null) {
                    classroomController.delete(id);
                    view.showMessage("Classroom deleted.");
                }
            }
            default -> view.showError("Choose one of the listed menu options.");
        }
    }

    private void saveClassroom(UUID id) {
        var details = view.readClassroomDetails(classroomController.listEquipment());
        if (details == null) {
            return;
        }
        if (id == null) {
            var created = classroomController.create(
                    details.name(), details.capacity(), details.equipment(), details.accessible());
            view.showMessage("Created classroom " + created.name() + " (" + created.id() + ").");
        } else {
            classroomController.update(id, details.name(), details.capacity(), details.equipment(), details.accessible());
            view.showMessage("Classroom updated.");
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
}
