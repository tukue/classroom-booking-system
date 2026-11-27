package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.ClassroomRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class JdbcClassroomRepository implements ClassroomRepository {
    private static final String SELECT = """
            SELECT c.classroom_id, c.name, c.seating_capacity, c.accessible,
                   e.equipment_id, e.name AS equipment_name
            FROM classrooms c
            LEFT JOIN classroom_equipment ce ON ce.classroom_id = c.classroom_id
            LEFT JOIN equipment e ON e.equipment_id = ce.equipment_id
            """;

    private final ConnectionFactory connections;

    public JdbcClassroomRepository(ConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections);
    }

    @Override
    public Classroom save(Classroom classroom) {
        write(classroom, """
                INSERT INTO classrooms (name, seating_capacity, accessible, classroom_id)
                VALUES (?, ?, ?, ?)
                """, false);
        return classroom;
    }

    @Override
    public void update(Classroom classroom) {
        write(classroom, """
                UPDATE classrooms SET name = ?, seating_capacity = ?, accessible = ?
                WHERE classroom_id = ?
                """, true);
    }

    private void write(Classroom classroom, String sql, boolean replaceEquipment) {
        try (Connection connection = connections.openConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, classroom.name());
                statement.setInt(2, classroom.seatingCapacity());
                statement.setBoolean(3, classroom.accessible());
                statement.setObject(4, classroom.id());
                requireClassroom(statement.executeUpdate());
                if (replaceEquipment) {
                    try (var remove = connection.prepareStatement(
                            "DELETE FROM classroom_equipment WHERE classroom_id = ?")) {
                        remove.setObject(1, classroom.id());
                        remove.executeUpdate();
                    }
                }
                insertEquipment(connection, classroom);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw switch (exception.getSQLState() == null ? "" : exception.getSQLState()) {
                case "23505" -> new IllegalArgumentException(
                        "A classroom with that name already exists.", exception);
                case "23503" -> new IllegalArgumentException(
                        "Selected equipment is no longer available.", exception);
                default -> new PersistenceException("Unable to save the classroom.", exception);
            };
        }
    }

    private void insertEquipment(Connection connection, Classroom classroom) throws SQLException {
        try (var statement = connection.prepareStatement(
                "INSERT INTO classroom_equipment (classroom_id, equipment_id) VALUES (?, ?)")) {
            for (Equipment item : classroom.equipment()) {
                statement.setObject(1, classroom.id());
                statement.setObject(2, item.id());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Override
    public Optional<Classroom> findById(UUID classroomId) {
        Objects.requireNonNull(classroomId);
        return read("WHERE c.classroom_id = ?", classroomId).stream().findFirst();
    }

    @Override
    public List<Classroom> findAll() {
        return read("", null);
    }

    private List<Classroom> read(String filter, UUID id) {
        try (var connection = connections.openConnection();
             var statement = connection.prepareStatement(SELECT + filter + " ORDER BY c.name")) {
            if (id != null) {
                statement.setObject(1, id);
            }
            try (var rows = statement.executeQuery()) {
                List<Classroom> classrooms = new ArrayList<>();
                boolean hasRow = rows.next();
                while (hasRow) {
                    UUID roomId = rows.getObject("classroom_id", UUID.class);
                    String name = rows.getString("name");
                    int capacity = rows.getInt("seating_capacity");
                    boolean accessible = rows.getBoolean("accessible");
                    Set<Equipment> equipment = new HashSet<>();
                    do {
                        UUID equipmentId = rows.getObject("equipment_id", UUID.class);
                        if (equipmentId != null) {
                            equipment.add(new Equipment(equipmentId, rows.getString("equipment_name")));
                        }
                        hasRow = rows.next();
                    } while (hasRow && roomId.equals(rows.getObject("classroom_id", UUID.class)));
                    classrooms.add(new Classroom(roomId, name, capacity, equipment, accessible));
                }
                return List.copyOf(classrooms);
            }
        } catch (SQLException exception) {
            throw new PersistenceException("Unable to load classrooms.", exception);
        }
    }

    @Override
    public void deleteById(UUID classroomId) {
        try (var connection = connections.openConnection();
             var statement = connection.prepareStatement("DELETE FROM classrooms WHERE classroom_id = ?")) {
            statement.setObject(1, classroomId);
            requireClassroom(statement.executeUpdate());
        } catch (SQLException exception) {
            if ("23503".equals(exception.getSQLState())) {
                throw new IllegalArgumentException("A classroom with bookings cannot be deleted.", exception);
            }
            throw new PersistenceException("Unable to delete the classroom.", exception);
        }
    }

    private void requireClassroom(int affectedRows) {
        if (affectedRows == 0) {
            throw new IllegalArgumentException("Classroom not found.");
        }
    }
}
