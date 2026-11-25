package com.tukue.classroombooking.domain.repository;

import com.tukue.classroombooking.domain.model.Classroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for the facility's managed training classrooms. */
public interface ClassroomRepository {
    Classroom save(Classroom classroom);

    Optional<Classroom> findById(UUID classroomId);

    List<Classroom> findAll();

    void update(Classroom classroom);

    void deleteById(UUID classroomId);
}
