package com.festdescans.api.domain.order;

import com.festdescans.api.domain.shared.InvalidDataException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public record Order(
        String id,
        Customer customer,
        List<OrderItem> items,
        OrderStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    public Order {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customer, "customer");
        items = items == null ? List.of() : List.copyOf(items);
    }

    /**
     * Places a PENDING order. Every line takes the current price of its product, which must be in
     * {@code products} (callers check for unknown products first).
     */
    public static Order place(
            String id,
            Customer customer,
            List<OrderLine> lines,
            Map<String, ProductSummary> products,
            String notes,
            Supplier<String> ids,
            Instant now) {
        if (lines == null || lines.isEmpty()) {
            throw new InvalidDataException("an order needs at least one item");
        }
        List<OrderItem> items = lines.stream()
                .map(line -> {
                    ProductSummary product = Objects.requireNonNull(products.get(line.productId()), line.productId());
                    return new OrderItem(ids.get(), id, product, line.quantity(), product.price());
                })
                .toList();
        return new Order(id, customer, items, OrderStatus.PENDING, notes, now, now);
    }

    public Order changeStatus(OrderStatus newStatus, Instant now) {
        return new Order(id, customer, items, Objects.requireNonNull(newStatus, "status"), notes, createdAt, now);
    }
}
