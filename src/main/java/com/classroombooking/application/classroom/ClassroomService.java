package com.classroombooking.application.classroom;

import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.ClassroomRepository;
import com.classroombooking.domain.repository.EquipmentRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClassroomService {
    private final ClassroomRepository classrooms;
    private final EquipmentRepository equipment;

    public ClassroomService(ClassroomRepository classrooms, EquipmentRepository equipment) {
        this.classrooms = classrooms;
        this.equipment = equipment;
    }

    public List<Classroom> listClassrooms() {
        return classrooms.findAll();
    }

    public List<Equipment> listEquipment() {
        return equipment.findAll();
    }

    public Classroom create(String name, int capacity, List<String> equipmentNames, boolean accessible) {
        return classrooms.save(new Classroom(UUID.randomUUID(), name, capacity,
                resolveEquipment(equipmentNames), accessible));
    }

    public void update(UUID id, String name, int capacity, List<String> equipmentNames, boolean accessible) {
        classrooms.update(new Classroom(id, name, capacity, resolveEquipment(equipmentNames), accessible));
    }

    public void delete(UUID id) {
        classrooms.deleteById(id);
    }

    private Set<Equipment> resolveEquipment(List<String> names) {
        List<Equipment> catalog = listEquipment();
        Set<Equipment> selected = new HashSet<>();
        for (String name : names) {
            selected.add(catalog.stream()
                    .filter(item -> item.name().equalsIgnoreCase(name.strip()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown equipment: " + name)));
        }
        return selected;
    }
}
