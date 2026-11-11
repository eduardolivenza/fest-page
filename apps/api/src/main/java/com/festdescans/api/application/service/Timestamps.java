package com.festdescans.api.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

final class Timestamps {

    private Timestamps() {
    }

    /** The database stores TIMESTAMP(3): keep millisecond precision so what we return is what is stored. */
    static Instant now(Clock clock) {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
