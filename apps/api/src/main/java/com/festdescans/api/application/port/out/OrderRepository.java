package com.festdescans.api.application.port.out;

import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.util.Optional;

public interface OrderRepository {

    /** Newest first; {@code status} null means all. */
    Page<Order> findPage(OrderStatus status, PageRequest page);

    Optional<Order> findById(String id);

    /** Inserts a new order with its items, or updates status/notes of an existing one. */
    Order save(Order order);
}
