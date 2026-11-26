package com.classroombooking.infrastructure.persistence;

import com.classroombooking.application.persistence.PersistenceException;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.ClassroomRepository;
import com.classroombooking.infrastructure.database.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads classroom attributes and equipment for room searches. */
public final class JdbcClassroomRepository implements ClassroomRepository {
    private static final Logger LOGGER = Logger.getLogger(JdbcClassroomRepository.class.getName());
    private static final String SELECT_CLASSROOMS = "SELECT c.classroom_id, c.name, "
            + "c.seating_capacity, c.accessible, e.equipment_id, e.name AS equipment_name "
            + "FROM classrooms c LEFT JOIN classroom_equipment ce "
            + "ON ce.classroom_id = c.classroom_id LEFT JOIN equipment e "
            + "ON e.equipment_id = ce.equipment_id";

    private final ConnectionFactory connectionFactory;

    public JdbcClassroomRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public Optional<Classroom> findById(UUID classroomId) {
        List<Classroom> classrooms = queryClassrooms(classroomId);
        return classrooms.stream().findFirst();
    }

    @Override
    public List<Classroom> findAll() {
        return queryClassrooms(null);
    }

    private List<Classroom> queryClassrooms(UUID classroomId) {
        String sql = SELECT_CLASSROOMS
                + (classroomId == null ? "" : " WHERE c.classroom_id = ?")
                + " ORDER BY c.name, e.name";
        Map<UUID, ClassroomBuilder> classrooms = new LinkedHashMap<>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (classroomId != null) {
                statement.setObject(1, classroomId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    UUID id = resultSet.getObject("classroom_id", UUID.class);
                    ClassroomBuilder classroom = classrooms.get(id);
                    if (classroom == null) {
                        classroom = new ClassroomBuilder(id, resultSet.getString("name"),
                                resultSet.getInt("seating_capacity"),
                                resultSet.getBoolean("accessible"));
                        classrooms.put(id, classroom);
                    }
                    UUID equipmentId = resultSet.getObject("equipment_id", UUID.class);
                    if (equipmentId != null) {
                        classroom.equipment.add(new Equipment(equipmentId,
                                resultSet.getString("equipment_name")));
                    }
                }
            }
            return classrooms.values().stream().map(ClassroomBuilder::build).toList();
        } catch (SQLException exception) {
            LOGGER.log(Level.FINE, "Could not load classroom search data.", exception);
            throw new PersistenceException("Classrooms could not be retrieved. Please try again.", exception);
        }
    }

    private static final class ClassroomBuilder {
        private final UUID id;
        private final String name;
        private final int capacity;
        private final boolean accessible;
        private final Set<Equipment> equipment = new LinkedHashSet<>();

        private ClassroomBuilder(UUID id, String name, int capacity, boolean accessible) {
            this.id = id;
            this.name = name;
            this.capacity = capacity;
            this.accessible = accessible;
        }

        private Classroom build() {
            return new Classroom(id, name, capacity, equipment, accessible);
        }
    }
}
