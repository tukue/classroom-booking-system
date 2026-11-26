package com.classroombooking.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassroomTest {
    private final Equipment projector = new Equipment(UUID.randomUUID(), "PROJECTOR");
    private final Equipment whiteboard = new Equipment(UUID.randomUUID(), "WHITEBOARD");

    @Test
    void meetsCapacityEquipmentAndAccessibilityRequirements() {
        Classroom classroom = new Classroom(UUID.randomUUID(), "Training 1", 20,
                Set.of(projector, whiteboard), true);

        assertTrue(classroom.meetsRequirements(new RoomRequirements(
                12, Set.of(projector), true)));
    }

    @Test
    void rejectsRequirementsTheRoomDoesNotMeet() {
        Classroom classroom = new Classroom(UUID.randomUUID(), "Training 2", 10,
                Set.of(projector), false);

        assertFalse(classroom.meetsRequirements(new RoomRequirements(
                12, Set.of(projector), false)));
        assertFalse(classroom.meetsRequirements(new RoomRequirements(
                8, Set.of(whiteboard), false)));
        assertFalse(classroom.meetsRequirements(new RoomRequirements(
                8, Set.of(), true)));
    }

    @Test
    void copiesEquipmentAndRequiresPositiveCapacity() {
        Set<Equipment> equipment = new HashSet<>(Set.of(projector));
        Classroom classroom = new Classroom(UUID.randomUUID(), "Training 3", 8,
                equipment, true);
        equipment.clear();

        assertTrue(classroom.equipment().contains(projector));
        assertThrows(IllegalArgumentException.class,
                () -> new Classroom(UUID.randomUUID(), "Invalid", 0, Set.of(), false));
    }
}
