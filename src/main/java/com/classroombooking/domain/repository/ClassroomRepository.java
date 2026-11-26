package com.classroombooking.domain.repository;

import com.classroombooking.domain.model.Classroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for the facility's managed training classrooms. */
public interface ClassroomRepository {
    Optional<Classroom> findById(UUID classroomId);

    List<Classroom> findAll();
}
