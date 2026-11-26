package com.classroombooking.presentation.console;

import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.domain.model.Customer;

import java.util.List;
import java.util.Objects;

/** Coordinates console customer actions with the customer application service. */
public final class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = Objects.requireNonNull(customerService);
    }

    public Customer registerCompany(String name, String email) {
        return customerService.registerCompany(name, email);
    }

    public Customer registerIndividual(String name, String email) {
        return customerService.registerIndividual(name, email);
    }

    public List<Customer> findAllCustomers() {
        return customerService.findAllCustomers();
    }
}
