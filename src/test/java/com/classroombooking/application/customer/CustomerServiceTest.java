package com.classroombooking.application.customer;

import com.classroombooking.domain.model.CompanyCustomer;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.IndividualCustomer;
import com.classroombooking.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerServiceTest {
    @Test
    void registersCompanyAndIndividualCustomersAndTrimsInput() {
        CustomerService service = new CustomerService(new InMemoryCustomerRepository());

        Customer company = service.registerCompany(" Acme Ltd ", " sales@acme.test ");
        Customer individual = service.registerIndividual(" Alex Customer ", " alex.customer@example.test ");

        assertInstanceOf(CompanyCustomer.class, company);
        assertEquals("Acme Ltd", company.name());
        assertEquals("sales@acme.test", company.email());
        assertInstanceOf(IndividualCustomer.class, individual);
        assertEquals("Alex Customer", individual.name());
        assertEquals(2, service.findAllCustomers().size());
    }

    @Test
    void rejectsBlankCustomerNameAndEmail() {
        CustomerService service = new CustomerService(new InMemoryCustomerRepository());

        assertThrows(IllegalArgumentException.class,
                () -> service.registerCompany("  ", "company@example.test"));
        assertThrows(IllegalArgumentException.class,
                () -> service.registerIndividual("Customer", " "));
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        private final List<Customer> customers = new ArrayList<>();

        @Override
        public Customer save(Customer customer) {
            customers.add(customer);
            return customer;
        }

        @Override
        public Optional<Customer> findById(UUID customerId) {
            return customers.stream().filter(customer -> customer.id().equals(customerId)).findFirst();
        }

        @Override
        public List<Customer> findAll() {
            return List.copyOf(customers);
        }

        @Override
        public void update(Customer customer) {
            deleteById(customer.id());
            customers.add(customer);
        }

        @Override
        public void deleteById(UUID customerId) {
            customers.removeIf(customer -> customer.id().equals(customerId));
        }
    }
}
