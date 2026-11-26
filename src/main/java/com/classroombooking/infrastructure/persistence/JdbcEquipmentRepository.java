package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.EquipmentRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcEquipmentRepository implements EquipmentRepository {
    private final ConnectionFactory connections;

    public JdbcEquipmentRepository(ConnectionFactory connections) {
        this.connections = connections;
    }

    @Override
    public List<Equipment> findAll() {
        try (var connection = connections.openConnection();
             var statement = connection.prepareStatement("SELECT equipment_id, name FROM equipment ORDER BY name");
             var rows = statement.executeQuery()) {
            List<Equipment> catalog = new ArrayList<>();
            while (rows.next()) {
                catalog.add(new Equipment(rows.getObject("equipment_id", UUID.class), rows.getString("name")));
            }
            return List.copyOf(catalog);
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to load the equipment catalog.", exception);
        }
    }
}
