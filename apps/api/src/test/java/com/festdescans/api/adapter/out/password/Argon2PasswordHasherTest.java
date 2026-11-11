package com.festdescans.api.adapter.out.password;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Argon2PasswordHasherTest {

    /** Created by the Node api (node-argon2 0.45 defaults) for "correct-password": as stored in production. */
    private static final String NODE_HASH =
            "$argon2id$v=19$m=65536,p=4,t=3$zF2Tfr2hu8qgJlk/NCcN9Q$/jI8aDO/MtX7fAD2Y5V8XEaAEngYZVY+zuuuFdP/NJE";

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

    @Test
    void verifiesHashesCreatedByTheNodeApi() {
        assertThat(hasher.matches("correct-password", NODE_HASH)).isTrue();
        assertThat(hasher.matches("wrong-password", NODE_HASH)).isFalse();
    }

    @Test
    void verifiesItsOwnHashes() {
        String hash = hasher.hash("s3cret!");

        assertThat(hash).startsWith("$argon2id$v=19$m=65536,t=3,p=4$");
        assertThat(hasher.matches("s3cret!", hash)).isTrue();
        assertThat(hasher.matches("s3cret", hash)).isFalse();
        assertThat(hasher.hash("s3cret!")).as("salted").isNotEqualTo(hash);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "plain-text",
            "$2b$10$abcdefghijklmnopqrstuv",
            "$argon2id$v=19$m=65536,t=3$c2FsdHNhbHQ$aGFzaGhhc2g",
            "$argon2id$v=19$m=99999999,t=3,p=4$c2FsdHNhbHQ$aGFzaGhhc2g",
            "$argon2id$v=19$m=65536,t=3,p=4$not base64!$aGFzaGhhc2g",
    })
    void rejectsMalformedHashesWithoutThrowing(String hash) {
        assertThat(hasher.matches("anything", hash)).isFalse();
    }
}
