package com.festdescans.api.domain.contact;

import java.time.Instant;

/** A message sent from the public contact form. */
public record ContactRequest(
        String id, String name, String email, String phone, String message, boolean read, Instant createdAt) {

    public static ContactRequest receive(String id, ContactMessage message, Instant now) {
        return new ContactRequest(id, message.name(), message.email(), message.phone(), message.message(), false, now);
    }

    public ContactRequest markRead() {
        return read ? this : new ContactRequest(id, name, email, phone, message, true, createdAt);
    }
}
