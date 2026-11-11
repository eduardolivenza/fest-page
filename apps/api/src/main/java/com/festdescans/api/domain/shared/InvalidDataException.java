package com.festdescans.api.domain.shared;

/** A business rule rejected the input (the adapters already validated its format). */
public final class InvalidDataException extends DomainException {

    public InvalidDataException(String message) {
        super(message);
    }
}
