package com.festdescans.api.adapter.in.web.contact;

import com.festdescans.api.domain.contact.ContactRequest;
import java.time.Instant;

public record ContactResponse(
        String id, String name, String email, String phone, String message, boolean read, Instant createdAt) {

    static ContactResponse from(ContactRequest c) {
        return new ContactResponse(c.id(), c.name(), c.email(), c.phone(), c.message(), c.read(), c.createdAt());
    }
}
