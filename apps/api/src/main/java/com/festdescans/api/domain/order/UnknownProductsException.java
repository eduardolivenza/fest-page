package com.festdescans.api.domain.order;

import com.festdescans.api.domain.shared.DomainException;

public final class UnknownProductsException extends DomainException {

    public UnknownProductsException() {
        super("One or more products not found");
    }
}
