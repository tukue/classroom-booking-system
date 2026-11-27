package com.classroombooking.presentation.console;

import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.application.roomsearch.RoomRequirementsFactory;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.EquipmentRepository;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/** Converts console room-search criteria into the application service request. */
public final class RoomSearchController {
    private final RoomSearchService roomSearchService;
    private final RoomRequirementsFactory requirementsFactory;

    public RoomSearchController(RoomSearchService roomSearchService,
                                EquipmentRepository equipmentRepository) {
        this(roomSearchService, new RoomRequirementsFactory(equipmentRepository));
    }

    public RoomSearchController(RoomSearchService roomSearchService,
                                RoomRequirementsFactory requirementsFactory) {
        this.roomSearchService = Objects.requireNonNull(roomSearchService);
        this.requirementsFactory = Objects.requireNonNull(requirementsFactory);
    }

    public List<Equipment> findAllEquipment() {
        return requirementsFactory.findAllEquipment();
    }

    public List<Classroom> findAvailableRooms(String startsAt, String endsAt, int requiredCapacity,
                                             String equipmentNames, boolean accessibilityRequired) {
        var requirements = requirementsFactory.create(requiredCapacity, equipmentNames, accessibilityRequired);
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

}
