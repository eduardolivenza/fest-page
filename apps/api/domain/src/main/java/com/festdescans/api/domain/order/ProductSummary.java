package com.festdescans.api.domain.order;

import java.math.BigDecimal;

/** The product an order line refers to, with its current price. */
public record ProductSummary(String id, String name, BigDecimal price) {
}
