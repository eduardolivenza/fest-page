package com.festdescans.api.domain.product;

import java.math.BigDecimal;
import java.util.List;

/** Data for a new product. {@code sizes} may be null (no sizes). */
public record ProductDraft(
        String name,
        String slug,
        String description,
        BigDecimal price,
        ProductCategory category,
        boolean featured,
        List<NewProductSize> sizes) {
}
