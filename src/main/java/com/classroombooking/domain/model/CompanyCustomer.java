package com.classroombooking.domain.model;

import java.util.UUID;

/** A company customer that rents training classrooms. */
public record CompanyCustomer(UUID id, String name, String email) implements Customer {
}
