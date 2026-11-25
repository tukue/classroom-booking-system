package com.classroombooking.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomRequirementsTest {
    @Test
    void copiesEquipmentCriteriaToPreventExternalMutation() {
        Equipment projector = new Equipment(UUID.randomUUID(), "PROJECTOR");
        Set<Equipment> equipment = new HashSet<>(Set.of(projector));
        RoomRequirements requirements = new RoomRequirements(12, equipment, true);

        equipment.clear();

        assertEquals(Set.of(projector), requirements.requiredEquipment());
        assertThrows(UnsupportedOperationException.class,
                () -> requirements.requiredEquipment().clear());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoomRequirements(0, Set.of(), false));
    }
}
