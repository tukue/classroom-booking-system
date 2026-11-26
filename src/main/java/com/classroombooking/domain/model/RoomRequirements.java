package com.classroombooking.domain.model;

import java.util.Set;

/** Search criteria a classroom must satisfy for a booking request. */
public record RoomRequirements(int requiredCapacity, Set<Equipment> requiredEquipment,
                               boolean accessibilityRequired) {
    public RoomRequirements {
        if (requiredCapacity <= 0) {
            throw new IllegalArgumentException("Required capacity must be positive.");
        }
        requiredEquipment = Set.copyOf(requiredEquipment);
    }
}
