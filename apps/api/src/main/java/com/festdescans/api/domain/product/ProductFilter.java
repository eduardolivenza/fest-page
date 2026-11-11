package com.festdescans.api.domain.product;

/** Public catalogue filter: {@code category} null means all; {@code featuredOnly} keeps featured products. */
public record ProductFilter(ProductCategory category, boolean featuredOnly) {

    public static ProductFilter none() {
        return new ProductFilter(null, false);
    }
}
