package com.festdescans.api.domain.order;

import com.festdescans.api.domain.shared.InvalidDataException;

/** One requested line of a new order. */
public record OrderLine(String productId, int quantity) {

    public OrderLine {
        if (productId == null || productId.isBlank()) {
            throw new InvalidDataException("productId is required");
        }
        if (quantity < 1) {
            throw new InvalidDataException("quantity must be at least 1");
        }
    }
}
