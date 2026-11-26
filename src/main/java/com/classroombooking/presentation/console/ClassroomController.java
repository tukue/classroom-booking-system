package com.classroombooking.presentation.console;

import com.classroombooking.application.classroom.ClassroomService;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;

import java.util.List;
import java.util.UUID;

/** Coordinates classroom inventory actions with the application service. */
public final class ClassroomController {
    private final ClassroomService classrooms;

    public ClassroomController(ClassroomService classrooms) {
        this.classrooms = classrooms;
    }

    public List<Classroom> listClassrooms() {
        return classrooms.listClassrooms();
    }

    public List<Equipment> listEquipment() {
        return classrooms.listEquipment();
    }

    public Classroom create(String name, int capacity, List<String> equipment, boolean accessible) {
        return classrooms.create(name, capacity, equipment, accessible);
    }

    public void update(UUID id, String name, int capacity, List<String> equipment, boolean accessible) {
        classrooms.update(id, name, capacity, equipment, accessible);
    }

    public void delete(UUID id) {
        classrooms.delete(id);
    }
}
