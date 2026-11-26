package com.classroombooking.domain.repository;

import com.classroombooking.domain.model.Equipment;

import java.util.List;

/** Persistence port for the equipment catalog used by classroom management and search. */
public interface EquipmentRepository {
    List<Equipment> findAll();
}
