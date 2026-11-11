package com.festdescans.api.adapter.in.web.common;

/** Shared constraint values for request records. */
public final class Validation {

    /** A dot in the domain is required (Bean Validation's @Email alone accepts "a@b"), as Zod's email() did. */
    public static final String EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private Validation() {
    }

    /** Validation group for fields required on create but optional on a partial update. */
    public interface OnCreate {
    }
}
