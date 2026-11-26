package com.classroombooking.presentation.console;

import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.BookingRepository;
import com.classroombooking.domain.repository.ClassroomRepository;
import com.classroombooking.domain.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomSearchControllerTest {
    @Test
    void resolvesEquipmentNamesAndParsesTimezoneAwareSearchTimes() {
        Equipment projector = new Equipment(UUID.randomUUID(), "PROJECTOR");
        Classroom classroom = new Classroom(UUID.randomUUID(), "Training Room", 20,
                Set.of(projector), true);
        RoomSearchController controller = controller(classroom, projector);

        List<Classroom> result = controller.findAvailableRooms(
                "2035-04-01T09:00:00Z", "2035-04-01T10:00:00Z", 16,
                " projector ", true);

        assertEquals(List.of(classroom), result);
    }

    @Test
    void rejectsUnknownEquipmentAndInvalidTimestamps() {
        Equipment projector = new Equipment(UUID.randomUUID(), "PROJECTOR");
        RoomSearchController controller = controller(
                new Classroom(UUID.randomUUID(), "Training Room", 20, Set.of(projector), true),
                projector);

        assertThrows(IllegalArgumentException.class, () -> controller.findAvailableRooms(
                "2035-04-01T09:00:00Z", "2035-04-01T10:00:00Z", 10,
                "LASER_CUTTER", false));
        assertThrows(IllegalArgumentException.class, () -> controller.findAvailableRooms(
                "not-a-time", "2035-04-01T10:00:00Z", 10, "", false));
    }

    private RoomSearchController controller(Classroom classroom, Equipment equipment) {
        ClassroomRepository classroomRepository = new ClassroomRepository() {
            @Override
            public Optional<Classroom> findById(UUID id) {
                return classroom.id().equals(id) ? Optional.of(classroom) : Optional.empty();
            }

            @Override
            public List<Classroom> findAll() {
                return List.of(classroom);
            }
        };
        BookingRepository bookingRepository = (startsAt, endsAt) -> List.<Booking>of();
        EquipmentRepository equipmentRepository = () -> List.of(equipment);
        return new RoomSearchController(new RoomSearchService(classroomRepository, bookingRepository),
                equipmentRepository);
    }
}
