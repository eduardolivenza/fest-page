package com.festdescans.api.domain.product;

import java.math.BigDecimal;

/** A size variant (width x length in cm) with its own price. */
public record ProductSize(String id, int width, int length, BigDecimal price, String productId) {
}
