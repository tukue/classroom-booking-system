package com.classroombooking.presentation.console;

import com.classroombooking.application.customer.CustomerService;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.repository.CustomerRepository;
import com.classroombooking.infrastructure.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMenuTest {
    @Test
    void registersListsAndExitsFromTheConsoleMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
        ConsoleMenu menu = menu("2\n1\n1\nAcme Ltd\nsales@acme.test\n"
                + "1\n2\nPat Customer\npat@example.test\n2\n0\n", output, repository);

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertTrue(display.contains("No customers registered."));
        assertTrue(display.contains("Registered customer Acme Ltd."));
        assertTrue(display.contains("Registered customer Pat Customer."));
        assertTrue(display.contains("sales@acme.test"));
        assertTrue(display.contains("pat@example.test"));
        assertTrue(display.contains("Goodbye."));
    }

    @Test
    void reportsInvalidMenuAndCustomerTypeChoices() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleMenu menu = menu("not a number\n1\n8\n0\n", output,
                new InMemoryCustomerRepository());

        menu.run();

        String display = output.toString(StandardCharsets.UTF_8);
        assertTrue(display.contains("Choose one of the listed menu options."));
        assertTrue(display.contains("Choose company or individual."));
    }

    @Test
    void reportsValidationAndPersistenceFailures() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleMenu invalidInputMenu = menu("1\n1\n \ncompany@example.test\n0\n",
                output, new InMemoryCustomerRepository());
        invalidInputMenu.run();
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Company name must not be blank."));

        ByteArrayOutputStream persistenceOutput = new ByteArrayOutputStream();
        ConsoleMenu persistenceFailureMenu = menu("1\n2\nPat Customer\npat@example.test\n0\n",
                persistenceOutput, new InMemoryCustomerRepository(true));
        persistenceFailureMenu.run();
        assertTrue(persistenceOutput.toString(StandardCharsets.UTF_8)
                .contains("Could not save customer."));
    }

    private ConsoleMenu menu(String input, ByteArrayOutputStream output,
                              InMemoryCustomerRepository repository) {
        ConsoleView view = new ConsoleView(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
        return new ConsoleMenu(new CustomerController(new CustomerService(repository)), view);
    }

    private static final class InMemoryCustomerRepository implements CustomerRepository {
        private final List<Customer> customers = new ArrayList<>();
        private final boolean failOnSave;

        private InMemoryCustomerRepository() {
            this(false);
        }

        private InMemoryCustomerRepository(boolean failOnSave) {
            this.failOnSave = failOnSave;
        }

        @Override
        public Customer save(Customer customer) {
            if (failOnSave) {
                throw new PersistenceException("Could not save customer.", null);
            }
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
