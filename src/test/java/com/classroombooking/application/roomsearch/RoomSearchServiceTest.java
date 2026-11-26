package com.classroombooking.application.roomsearch;

import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.Equipment;
import com.classroombooking.domain.model.RoomRequirements;
import com.classroombooking.domain.repository.BookingAvailabilityRepository;
import com.classroombooking.domain.repository.ClassroomRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomSearchServiceTest {
    @Test
    void filtersRequirementsAndOverlapsThenSortsByRoomName() {
        Equipment projector = new Equipment(UUID.randomUUID(), "PROJECTOR");
        Equipment whiteboard = new Equipment(UUID.randomUUID(), "WHITEBOARD");
        Classroom zulu = room("Zulu Room", 24, Set.of(projector, whiteboard), true);
        Classroom alpha = room("Alpha Room", 30, Set.of(projector), true);
        Classroom tooSmall = room("Small Room", 10, Set.of(projector), true);
        Classroom inaccessible = room("Inaccessible Room", 35, Set.of(projector), false);
        Classroom missingEquipment = room("No Projector", 40, Set.of(whiteboard), true);
        Classroom booked = room("Booked Room", 28, Set.of(projector), true);
        OffsetDateTime startsAt = time("09:00:00Z");
        OffsetDateTime endsAt = time("10:00:00Z");
        Booking overlapping = new Booking(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                booked.id(), time("09:30:00Z"), time("10:30:00Z"), null);
        RoomSearchService service = new RoomSearchService(
                new InMemoryClassroomRepository(List.of(zulu, tooSmall, booked, inaccessible,
                        missingEquipment, alpha)),
                new InMemoryBookingRepository(List.of(overlapping)));

        List<Classroom> available = service.findAvailableRooms(
                new RoomRequirements(20, Set.of(projector), true), startsAt, endsAt);

        assertEquals(List.of(alpha, zulu), available);
    }

    @Test
    void rejectsInvalidSearchInterval() {
        RoomSearchService service = new RoomSearchService(
                new InMemoryClassroomRepository(List.of()),
                new InMemoryBookingRepository(List.of()));

        assertThrows(IllegalArgumentException.class,
                () -> service.findAvailableRooms(new RoomRequirements(1, Set.of(), false),
                        time("10:00:00Z"), time("10:00:00Z")));
    }

    private Classroom room(String name, int capacity, Set<Equipment> equipment, boolean accessible) {
        return new Classroom(UUID.randomUUID(), name, capacity, equipment, accessible);
    }

    private OffsetDateTime time(String time) {
        return OffsetDateTime.parse("2035-04-01T" + time);
    }

    private static final class InMemoryClassroomRepository implements ClassroomRepository {
        private final List<Classroom> classrooms;

        private InMemoryClassroomRepository(List<Classroom> classrooms) {
            this.classrooms = new ArrayList<>(classrooms);
        }

        @Override
        public Classroom save(Classroom classroom) {
            classrooms.add(classroom);
            return classroom;
        }

        @Override
        public Optional<Classroom> findById(UUID classroomId) {
            return classrooms.stream().filter(room -> room.id().equals(classroomId)).findFirst();
        }

        @Override
        public List<Classroom> findAll() {
            return List.copyOf(classrooms);
        }

        @Override
        public void update(Classroom classroom) {
            deleteById(classroom.id());
            classrooms.add(classroom);
        }

        @Override
        public void deleteById(UUID classroomId) {
            classrooms.removeIf(classroom -> classroom.id().equals(classroomId));
        }
    }

    private record InMemoryBookingRepository(List<Booking> overlapping)
            implements BookingAvailabilityRepository {
        @Override
        public List<Booking> findOverlapping(OffsetDateTime startsAt, OffsetDateTime endsAt) {
            return overlapping.stream()
                    .filter(booking -> booking.overlaps(startsAt, endsAt))
                    .toList();
        }
    }
}
