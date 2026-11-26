package com.classroombooking.domain.repository;

import com.classroombooking.domain.model.Customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for external company and individual customers. */
public interface CustomerRepository {
    Customer save(Customer customer);

    Optional<Customer> findById(UUID customerId);

    List<Customer> findAll();

    void update(Customer customer);

    void deleteById(UUID customerId);
}
