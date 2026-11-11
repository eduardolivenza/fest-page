package com.festdescans.api.domain.product;

import java.math.BigDecimal;
import java.util.List;

/**
 * Partial update: a null field is left unchanged. A non-null {@code sizes} replaces all sizes
 * (an empty list removes them).
 */
public record ProductChanges(
        String name,
        String slug,
        String description,
        BigDecimal price,
        ProductCategory category,
        Boolean featured,
        List<NewProductSize> sizes) {
}
