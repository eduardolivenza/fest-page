package com.festdescans.api.application.service;

import com.festdescans.api.application.port.in.ManageOrdersUseCase;
import com.festdescans.api.application.port.in.PlaceOrderUseCase;
import com.festdescans.api.application.port.out.CustomerRepository;
import com.festdescans.api.application.port.out.IdGenerator;
import com.festdescans.api.application.port.out.OrderRepository;
import com.festdescans.api.application.port.out.ProductRepository;
import com.festdescans.api.application.port.out.TransactionRunner;
import com.festdescans.api.domain.order.Customer;
import com.festdescans.api.domain.order.CustomerDetails;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderLine;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.order.ProductSummary;
import com.festdescans.api.domain.order.UnknownProductsException;
import com.festdescans.api.domain.shared.NotFoundException;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class OrderService implements PlaceOrderUseCase, ManageOrdersUseCase {

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final IdGenerator ids;
    private final TransactionRunner tx;
    private final Clock clock;

    public OrderService(OrderRepository orders, CustomerRepository customers, ProductRepository products,
            IdGenerator ids, TransactionRunner tx, Clock clock) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.ids = ids;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public Order placeOrder(CustomerDetails details, List<OrderLine> lines, String notes) {
        return tx.inTransaction(() -> {
            List<String> productIds = lines.stream().map(OrderLine::productId).toList();
            Map<String, ProductSummary> found = products.findAllById(productIds).stream()
                    .map(p -> new ProductSummary(p.id(), p.name(), p.price()))
                    .collect(Collectors.toMap(ProductSummary::id, Function.identity()));
            // Same rule as before the Java migration: one line per product, all of them must exist.
            if (found.size() != productIds.size()) {
                throw new UnknownProductsException();
            }

            Instant now = Timestamps.now(clock);
            Customer customer = customers.save(customers.findByEmail(details.email())
                    .map(existing -> existing.refresh(details, now))
                    .orElseGet(() -> Customer.register(ids.newId(), details, now)));

            return orders.save(Order.place(ids.newId(), customer, lines, found, notes, ids::newId, now));
        });
    }

    @Override
    public Page<Order> listOrders(OrderStatus status, PageRequest page) {
        return orders.findPage(status, page);
    }

    @Override
    public Order changeOrderStatus(String id, OrderStatus status) {
        return tx.inTransaction(() -> {
            Order order = orders.findById(id).orElseThrow(() -> new NotFoundException("Order", id));
            return orders.save(order.changeStatus(status, Timestamps.now(clock)));
        });
    }
}
