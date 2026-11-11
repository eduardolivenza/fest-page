package com.festdescans.api.application.port.out;

public interface PasswordVerifier {

    /** Constant-time check of {@code rawPassword} against a stored hash; false for a malformed hash. */
    boolean matches(String rawPassword, String passwordHash);
}
