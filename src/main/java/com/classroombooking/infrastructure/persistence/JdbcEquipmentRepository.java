package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.EquipmentRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Reads equipment options used by classroom search. */
public final class JdbcEquipmentRepository implements EquipmentRepository {
    private static final Logger LOGGER = Logger.getLogger(JdbcEquipmentRepository.class.getName());
    private final ConnectionFactory connectionFactory;

    public JdbcEquipmentRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public List<Equipment> findAll() {
        List<Equipment> equipment = new ArrayList<>();
        String sql = "SELECT equipment_id, name FROM equipment ORDER BY name";
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                equipment.add(new Equipment(resultSet.getObject("equipment_id", UUID.class),
                        resultSet.getString("name")));
            }
            return List.copyOf(equipment);
        } catch (SQLException exception) {
            LOGGER.log(Level.FINE, "Could not load equipment search options.", exception);
            throw new PersistenceException("Equipment options could not be retrieved. Please try again.",
                    exception);
        }
    }
}
