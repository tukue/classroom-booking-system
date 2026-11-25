package com.tukue.classroombooking.domain.model;

import java.util.UUID;

/** Common identity and contact contract for external company and individual customers. */
public sealed interface Customer permits CompanyCustomer, IndividualCustomer {
    UUID id();

    String name();

    String email();
}
