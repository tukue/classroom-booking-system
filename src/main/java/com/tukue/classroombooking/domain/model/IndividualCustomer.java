package com.tukue.classroombooking.domain.model;

import java.util.UUID;

/** An individual customer that rents training classrooms. */
public record IndividualCustomer(UUID id, String name, String email) implements Customer {
}
