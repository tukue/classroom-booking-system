package com.classroombooking.domain.repository;

import com.classroombooking.domain.model.Booking;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Reads upcoming bookings in ascending start-time order for a selected room or customer. */
public interface BookingScheduleRepository {
    List<Booking> findUpcomingByClassroom(UUID classroomId, OffsetDateTime from);

    List<Booking> findUpcomingByCustomer(UUID customerId, OffsetDateTime from);
}
