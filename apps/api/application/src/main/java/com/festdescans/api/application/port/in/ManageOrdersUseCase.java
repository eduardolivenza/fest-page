package com.festdescans.api.application.port.in;

import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;

public interface ManageOrdersUseCase {

    /** Newest first. {@code status} null means all. */
    Page<Order> listOrders(OrderStatus status, PageRequest page);

    /** @throws com.festdescans.api.domain.shared.NotFoundException if there is no such order */
    Order changeOrderStatus(String id, OrderStatus status);
}
