package com.festdescans.api.domain.order;

/** Keep in sync with the PostgreSQL enum "OrderStatus" and packages/types. */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    DELIVERED,
    CANCELLED
}
