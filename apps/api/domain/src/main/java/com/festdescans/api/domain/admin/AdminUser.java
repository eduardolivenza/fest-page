package com.festdescans.api.domain.admin;

import java.time.Instant;

/** A back-office user. {@code passwordHash} is an Argon2id PHC string and never leaves the api. */
public record AdminUser(
        String id, String email, String passwordHash, String name, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "AdminUser[id=" + id + ", email=" + email + ", name=" + name + "]";
    }
}
