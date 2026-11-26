package com.classroombooking.presentation.console;

import com.classroombooking.domain.model.Customer;
import com.classroombooking.infrastructure.persistence.PersistenceException;

import java.util.Objects;

/** Runs the minimal console menu for customer registration and listing. */
public final class ConsoleMenu {
    private final CustomerController customerController;
    private final ConsoleView view;

    public ConsoleMenu(CustomerController customerController, ConsoleView view) {
        this.customerController = Objects.requireNonNull(customerController);
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
}
