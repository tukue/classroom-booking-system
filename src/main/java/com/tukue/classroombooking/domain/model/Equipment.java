package com.tukue.classroombooking.domain.model;

import java.util.UUID;

/** A named equipment item available in one or more classrooms. */
public record Equipment(UUID id, String name) {
    public Equipment {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Equipment name must not be blank.");
        }
    }
}
