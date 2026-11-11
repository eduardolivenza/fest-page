package com.festdescans.api.domain.order;

import java.time.Instant;

/** A customer is identified by email: placing a new order refreshes the name and phone. */
public record Customer(String id, String name, String email, String phone, Instant createdAt, Instant updatedAt) {

    public static Customer register(String id, CustomerDetails details, Instant now) {
        return new Customer(id, details.name(), details.email(), details.phone(), now, now);
    }

    public Customer refresh(CustomerDetails details, Instant now) {
        return new Customer(id, details.name(), email, details.phone(), createdAt, now);
    }
}
