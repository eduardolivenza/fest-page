package com.festdescans.api.domain.shared;

public final class NotFoundException extends DomainException {

    public NotFoundException(String what, String id) {
        super(what + " " + id + " not found");
    }
}
