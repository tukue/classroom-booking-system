package com.classroombooking.application.roomsearch;

import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.model.RoomRequirements;
import com.classroombooking.domain.repository.EquipmentRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds validated room requirements from equipment names entered in the console. */
public final class RoomRequirementsFactory {
    private final EquipmentRepository equipmentRepository;

    public RoomRequirementsFactory(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = Objects.requireNonNull(equipmentRepository);
    }

    public List<Equipment> findAllEquipment() {
        return equipmentRepository.findAll();
    }

    public RoomRequirements create(int requiredCapacity, String equipmentNames,
                                   boolean accessibilityRequired) {
        return new RoomRequirements(requiredCapacity, resolveEquipment(equipmentNames),
                accessibilityRequired);
    }

    private Set<Equipment> resolveEquipment(String input) {
        if (input == null || input.isBlank()) {
            return Set.of();
        }
        Map<String, List<Equipment>> catalog = equipmentRepository.findAll().stream()
                .collect(Collectors.groupingBy(equipment -> normalize(equipment.name())));
        Set<Equipment> selected = new LinkedHashSet<>();
        for (String item : input.split(",")) {
            if (item.isBlank()) {
                continue;
            }
            String name = normalize(item);
            List<Equipment> matches = catalog.get(name);
            if (matches == null) {
                throw new IllegalArgumentException("Unknown equipment: " + item.strip());
            }
            if (matches.size() > 1) {
                throw new IllegalArgumentException("Equipment name is ambiguous: " + item.strip());
            }
            selected.add(matches.get(0));
        }
        return Set.copyOf(selected);
    }

    private String normalize(String value) {
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
