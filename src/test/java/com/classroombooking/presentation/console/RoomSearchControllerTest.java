package com.classroombooking.presentation.console;

import com.classroombooking.application.roomsearch.RoomSearchService;
import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.repository.BookingAvailabilityRepository;
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
                "2026-12-02T09:00:00Z", "2026-12-02T10:00:00Z", 16,
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
                "2026-12-02T09:00:00Z", "2026-12-02T10:00:00Z", 10,
                "LASER_CUTTER", false));
        assertThrows(IllegalArgumentException.class, () -> controller.findAvailableRooms(
                "not-a-time", "2026-12-02T10:00:00Z", 10, "", false));
    }

    @Test
    void unrelatedEquipmentCanBeSelectedWhenOtherNamesNormalizeToDuplicates() {
        Equipment projectorUppercase = new Equipment(UUID.randomUUID(), "PROJECTOR");
        Equipment projectorLowercase = new Equipment(UUID.randomUUID(), "projector");
        Equipment whiteboard = new Equipment(UUID.randomUUID(), "WHITEBOARD");
        Classroom classroom = new Classroom(UUID.randomUUID(), "Whiteboard Room", 20,
                Set.of(whiteboard), true);
        RoomSearchController controller = controller(classroom,
                List.of(projectorUppercase, projectorLowercase, whiteboard));

        List<Classroom> result = controller.findAvailableRooms(
                "2026-12-02T09:00:00Z", "2026-12-02T10:00:00Z", 10,
                "WHITEBOARD", false);

        assertEquals(List.of(classroom), result);
    }

    @Test
    void rejectsAnEquipmentNameThatIsAmbiguousWhenRequested() {
        Equipment projectorUppercase = new Equipment(UUID.randomUUID(), "PROJECTOR");
        Equipment projectorLowercase = new Equipment(UUID.randomUUID(), "projector");
        Classroom classroom = new Classroom(UUID.randomUUID(), "Projector Room", 20,
                Set.of(projectorUppercase), true);
        RoomSearchController controller = controller(classroom,
                List.of(projectorUppercase, projectorLowercase));

        assertThrows(IllegalArgumentException.class, () -> controller.findAvailableRooms(
                "2026-12-02T09:00:00Z", "2026-12-02T10:00:00Z", 10,
                "PROJECTOR", false));
    }

    private RoomSearchController controller(Classroom classroom, Equipment equipment) {
        return controller(classroom, List.of(equipment));
    }

    private RoomSearchController controller(Classroom classroom, List<Equipment> equipment) {
        ClassroomRepository classroomRepository = new ClassroomRepository() {
            @Override
            public Classroom save(Classroom room) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Classroom> findById(UUID id) {
                return classroom.id().equals(id) ? Optional.of(classroom) : Optional.empty();
            }

            @Override
            public List<Classroom> findAll() {
                return List.of(classroom);
            }

            @Override
            public void update(Classroom room) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteById(UUID id) {
                throw new UnsupportedOperationException();
            }
        };
        BookingAvailabilityRepository bookingRepository = (startsAt, endsAt) -> List.<Booking>of();
        EquipmentRepository equipmentRepository = () -> equipment;
        return new RoomSearchController(new RoomSearchService(classroomRepository, bookingRepository),
                equipmentRepository);
    }
}
