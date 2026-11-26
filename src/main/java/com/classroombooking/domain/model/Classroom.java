package com.classroombooking.domain.model;

import java.util.Set;
import java.util.UUID;

/** A training room and the attributes used to determine whether it fits a booking request. */
public record Classroom(UUID id, String name, int seatingCapacity, Set<Equipment> equipment,
                        boolean accessible) {
    public Classroom {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Classroom name must not be blank.");
        }
        name = name.strip();
        if (name.codePointCount(0, name.length()) > 100) {
            throw new IllegalArgumentException("Classroom name must be at most 100 characters.");
        }
        if (seatingCapacity <= 0) {
            throw new IllegalArgumentException("Seating capacity must be positive.");
        }
        equipment = Set.copyOf(equipment);
    }

    /** Returns whether this classroom satisfies capacity, equipment, and accessibility criteria. */
    public boolean meetsRequirements(RoomRequirements requirements) {
        return seatingCapacity >= requirements.requiredCapacity()
                && equipment.containsAll(requirements.requiredEquipment())
                && (!requirements.accessibilityRequired() || accessible);
    }
}
