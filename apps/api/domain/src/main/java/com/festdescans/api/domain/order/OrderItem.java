package com.festdescans.api.domain.order;

import java.math.BigDecimal;

/** {@code unitPrice} is the product price when the order was placed; later price changes don't affect it. */
public record OrderItem(String id, String orderId, ProductSummary product, int quantity, BigDecimal unitPrice) {
}
