package com.classroombooking.presentation.console;

import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.model.RoomRequirements;
import com.classroombooking.domain.repository.EquipmentRepository;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Converts console room-search criteria into the application service request. */
public final class RoomSearchController {
    private final RoomSearchService roomSearchService;
    private final EquipmentRepository equipmentRepository;

    public RoomSearchController(RoomSearchService roomSearchService,
                                EquipmentRepository equipmentRepository) {
        this.roomSearchService = Objects.requireNonNull(roomSearchService);
        this.equipmentRepository = Objects.requireNonNull(equipmentRepository);
    }

    public List<Equipment> findAllEquipment() {
        return equipmentRepository.findAll();
    }

    public List<Classroom> findAvailableRooms(String startsAt, String endsAt, int requiredCapacity,
                                             String equipmentNames, boolean accessibilityRequired) {
        RoomRequirements requirements = new RoomRequirements(requiredCapacity,
                resolveEquipment(equipmentNames), accessibilityRequired);
        return roomSearchService.findAvailableRooms(requirements,
                parseTime(startsAt), parseTime(endsAt));
    }

    private OffsetDateTime parseTime(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Enter date and time in ISO-8601 format with a timezone offset.");
        }
    }

    private Set<Equipment> resolveEquipment(String input) {
        if (input == null || input.isBlank()) {
            return Set.of();
        }
        Map<String, List<Equipment>> catalog = equipmentRepository.findAll().stream()
                .collect(Collectors.groupingBy(equipment -> normalize(equipment.name())));
        Set<Equipment> requiredEquipment = new LinkedHashSet<>();
        for (String item : input.split(",")) {
            String name = normalize(item);
            List<Equipment> matches = catalog.get(name);
            if (matches == null) {
                throw new IllegalArgumentException("Unknown equipment: " + item.strip());
            }
            if (matches.size() > 1) {
                throw new IllegalArgumentException("Equipment name is ambiguous: " + item.strip());
            }
            requiredEquipment.add(matches.get(0));
        }
        return Set.copyOf(requiredEquipment);
    }

    private String normalize(String value) {
        return value.strip().toUpperCase(Locale.ROOT);
    }
}
