package com.tukue.classroombooking.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerTest {
    @Test
    void companyAndIndividualCustomersShareTheCustomerContract() {
        UUID companyId = UUID.randomUUID();
        UUID individualId = UUID.randomUUID();
        List<Customer> customers = List.of(
                new CompanyCustomer(companyId, "Example Company", "company@example.test"),
                new IndividualCustomer(individualId, "Example Person", "person@example.test"));

        assertEquals(companyId, customers.get(0).id());
        assertEquals("Example Company", customers.get(0).name());
        assertEquals("company@example.test", customers.get(0).email());
        assertEquals(individualId, customers.get(1).id());
        assertEquals("Example Person", customers.get(1).name());
        assertEquals("person@example.test", customers.get(1).email());
    }
}
