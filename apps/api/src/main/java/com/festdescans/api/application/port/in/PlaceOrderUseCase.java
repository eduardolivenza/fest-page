package com.festdescans.api.application.port.in;

import com.festdescans.api.domain.order.CustomerDetails;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderLine;
import java.util.List;

public interface PlaceOrderUseCase {

    /**
     * Creates or refreshes the customer (by email) and places a PENDING order priced with the
     * current product prices.
     *
     * @throws com.festdescans.api.domain.order.UnknownProductsException if a product does not exist
     */
    Order placeOrder(CustomerDetails customer, List<OrderLine> lines, String notes);
}
