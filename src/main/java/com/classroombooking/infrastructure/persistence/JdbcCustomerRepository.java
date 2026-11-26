package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.CompanyCustomer;
import com.classroombooking.domain.model.Customer;
import com.classroombooking.domain.model.IndividualCustomer;
import com.classroombooking.domain.repository.CustomerRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** JDBC implementation of customer persistence operations. */
public final class JdbcCustomerRepository implements CustomerRepository {
    private static final Logger LOGGER = Logger.getLogger(JdbcCustomerRepository.class.getName());
    private final ConnectionFactory connectionFactory;

    public JdbcCustomerRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public Customer save(Customer customer) {
        String sql = "INSERT INTO customers (customer_id, customer_type, display_name, email) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, customer.id());
            statement.setString(2, customerType(customer));
            statement.setString(3, customer.name());
            statement.setString(4, customer.email());
            statement.executeUpdate();
            return customer;
        } catch (SQLException exception) {
            throw persistenceFailure("Customer could not be saved. Please try again.", exception);
        }
    }

    @Override
    public Optional<Customer> findById(UUID customerId) {
        String sql = "SELECT customer_id, customer_type, display_name, email "
                + "FROM customers WHERE customer_id = ?";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapCustomer(resultSet)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw persistenceFailure("Customer could not be retrieved. Please try again.", exception);
        }
    }

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT customer_id, customer_type, display_name, email "
                + "FROM customers ORDER BY display_name";
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                customers.add(mapCustomer(resultSet));
            }
            return List.copyOf(customers);
        } catch (SQLException exception) {
            throw persistenceFailure("Customers could not be listed. Please try again.", exception);
        }
    }

    @Override
    public void update(Customer customer) {
        String sql = "UPDATE customers SET customer_type = ?, display_name = ?, email = ? "
                + "WHERE customer_id = ?";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerType(customer));
            statement.setString(2, customer.name());
            statement.setString(3, customer.email());
            statement.setObject(4, customer.id());
            if (statement.executeUpdate() == 0) {
                throw new PersistenceException("Customer was not found.", null);
            }
        } catch (SQLException exception) {
            throw persistenceFailure("Customer could not be updated. Please try again.", exception);
        }
    }

    @Override
    public void deleteById(UUID customerId) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, customerId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw persistenceFailure("Customer could not be deleted. Please try again.", exception);
        }
    }

    private Customer mapCustomer(ResultSet resultSet) throws SQLException {
        UUID id = resultSet.getObject("customer_id", UUID.class);
        String name = resultSet.getString("display_name");
        String email = resultSet.getString("email");
        return switch (resultSet.getString("customer_type")) {
            case "COMPANY" -> new CompanyCustomer(id, name, email);
            case "INDIVIDUAL" -> new IndividualCustomer(id, name, email);
            default -> throw new SQLException("Unknown customer type in database.");
        };
    }

    private String customerType(Customer customer) {
        if (customer instanceof CompanyCustomer) {
            return "COMPANY";
        }
        if (customer instanceof IndividualCustomer) {
            return "INDIVIDUAL";
        }
        throw new IllegalArgumentException("Unsupported customer type: " + customer.getClass().getName());
    }

    private PersistenceException persistenceFailure(String userMessage, SQLException cause) {
        LOGGER.log(Level.FINE, userMessage, cause);
        return new PersistenceException(userMessage, cause);
    }
}
