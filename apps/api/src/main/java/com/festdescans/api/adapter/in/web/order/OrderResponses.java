package com.festdescans.api.adapter.in.web.order;

import com.festdescans.api.domain.order.Customer;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderItem;
import com.festdescans.api.domain.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** The order shapes each endpoint returns (same fields as the Node api; prices as JSON numbers). */
final class OrderResponses {

    private OrderResponses() {
    }

    /** PATCH /api/admin/orders/{id}/status. */
    record OrderResponse(String id, String customerId, OrderStatus status, String notes, Instant createdAt,
            Instant updatedAt) {

        static OrderResponse from(Order o) {
            return new OrderResponse(o.id(), o.customer().id(), o.status(), o.notes(), o.createdAt(), o.updatedAt());
        }
    }

    /** POST /api/orders. */
    record PlacedOrderResponse(String id, String customerId, OrderStatus status, String notes, Instant createdAt,
            Instant updatedAt, List<ItemResponse> items) {

        static PlacedOrderResponse from(Order o) {
            return new PlacedOrderResponse(o.id(), o.customer().id(), o.status(), o.notes(), o.createdAt(),
                    o.updatedAt(), o.items().stream().map(ItemResponse::from).toList());
        }
    }

    record ItemResponse(String id, String orderId, String productId, int quantity, BigDecimal unitPrice) {

        static ItemResponse from(OrderItem i) {
            return new ItemResponse(i.id(), i.orderId(), i.product().id(), i.quantity(), i.unitPrice());
        }
    }

    /** GET /api/admin/orders: with the customer and each item's product. */
    record AdminOrderResponse(String id, String customerId, OrderStatus status, String notes, Instant createdAt,
            Instant updatedAt, CustomerResponse customer, List<AdminItemResponse> items) {

        static AdminOrderResponse from(Order o) {
            return new AdminOrderResponse(o.id(), o.customer().id(), o.status(), o.notes(), o.createdAt(),
                    o.updatedAt(), CustomerResponse.from(o.customer()),
                    o.items().stream().map(AdminItemResponse::from).toList());
        }
    }

    record CustomerResponse(String id, String name, String email, String phone, Instant createdAt,
            Instant updatedAt) {

        static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.id(), c.name(), c.email(), c.phone(), c.createdAt(), c.updatedAt());
        }
    }

    record AdminItemResponse(String id, String orderId, String productId, int quantity, BigDecimal unitPrice,
            ProductRef product) {

        static AdminItemResponse from(OrderItem i) {
            return new AdminItemResponse(i.id(), i.orderId(), i.product().id(), i.quantity(), i.unitPrice(),
                    new ProductRef(i.product().id(), i.product().name(), i.product().price()));
        }
    }

    record ProductRef(String id, String name, BigDecimal price) {
    }
}
