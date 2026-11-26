package com.classroombooking.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipmentTest {
    @Test
    void requiresNonBlankEquipmentName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Equipment(UUID.randomUUID(), " "));
    }

    @Test
    void storesEquipmentIdentifierAndName() {
        UUID id = UUID.randomUUID();
        Equipment equipment = new Equipment(id, "PROJECTOR");

        assertEquals(id, equipment.id());
        assertEquals("PROJECTOR", equipment.name());
    }
}
