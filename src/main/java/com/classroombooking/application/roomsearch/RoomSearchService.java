package com.classroombooking.application.roomsearch;

import com.classroombooking.domain.model.Booking;
import com.classroombooking.domain.model.Classroom;
import com.classroombooking.domain.model.RoomRequirements;
import com.classroombooking.domain.repository.BookingRepository;
import com.classroombooking.domain.repository.ClassroomRepository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Finds classrooms that meet room requirements and have no overlapping bookings. */
public final class RoomSearchService {
    private final ClassroomRepository classroomRepository;
    private final BookingRepository bookingRepository;

    public RoomSearchService(ClassroomRepository classroomRepository,
                             BookingRepository bookingRepository) {
        this.classroomRepository = Objects.requireNonNull(classroomRepository);
        this.bookingRepository = Objects.requireNonNull(bookingRepository);
    }

    public List<Classroom> findAvailableRooms(RoomRequirements requirements,
                                              OffsetDateTime startsAt,
                                              OffsetDateTime endsAt) {
        Objects.requireNonNull(requirements, "requirements");
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Search start time must be before end time.");
        }

        Set<UUID> occupiedClassrooms = bookingRepository.findOverlapping(startsAt, endsAt).stream()
                .map(Booking::classroomId)
                .collect(Collectors.toSet());

        return classroomRepository.findAll().stream()
                .filter(classroom -> classroom.meetsRequirements(requirements))
                .filter(classroom -> !occupiedClassrooms.contains(classroom.id()))
                .sorted(Comparator.comparing(Classroom::name))
                .toList();
    }
}
