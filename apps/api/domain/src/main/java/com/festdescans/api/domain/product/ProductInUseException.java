package com.festdescans.api.domain.product;

import com.festdescans.api.domain.shared.DomainException;

/** A product referenced by order items cannot be deleted (orders keep their history). */
public final class ProductInUseException extends DomainException {

    public ProductInUseException(String productId) {
        super("Product " + productId + " has orders");
    }
}
