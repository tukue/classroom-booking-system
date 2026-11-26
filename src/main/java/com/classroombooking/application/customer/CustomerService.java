package com.classroombooking.application.customer;

import com.classroombooking.domain.model.CompanyCustomer;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.IndividualCustomer;
import com.classroombooking.domain.repository.CustomerRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Implements customer registration and listing use cases. */
public final class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
    }

    public Customer registerCompany(String name, String email) {
        return customerRepository.save(new CompanyCustomer(
                UUID.randomUUID(), requireText(name, "Company name"), requireText(email, "Email")));
    }

    public Customer registerIndividual(String name, String email) {
        return customerRepository.save(new IndividualCustomer(
                UUID.randomUUID(), requireText(name, "Customer name"), requireText(email, "Email")));
    }

    public List<Customer> findAllCustomers() {
        return List.copyOf(customerRepository.findAll());
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.strip();
    }
}
