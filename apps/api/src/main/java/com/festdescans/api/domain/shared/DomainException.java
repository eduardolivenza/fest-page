package com.festdescans.api.domain.shared;

/** Base class of the business errors that adapters translate (e.g. to HTTP status codes). */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
