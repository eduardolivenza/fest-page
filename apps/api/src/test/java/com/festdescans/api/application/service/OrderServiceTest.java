package com.festdescans.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.application.fakes.DirectTransactionRunner;
import com.festdescans.api.application.fakes.Fixtures;
import com.festdescans.api.application.fakes.InMemoryDatabase;
import com.festdescans.api.application.fakes.MutableClock;
import com.festdescans.api.application.fakes.SequentialIdGenerator;
import com.festdescans.api.domain.order.CustomerDetails;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderLine;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.order.UnknownProductsException;
import com.festdescans.api.domain.shared.NotFoundException;
import com.festdescans.api.domain.shared.PageRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private final InMemoryDatabase db = new InMemoryDatabase();
    private final DirectTransactionRunner tx = new DirectTransactionRunner();
    private final OrderService service = new OrderService(db.orderRepository, db.customerRepository,
            db.productRepository, new SequentialIdGenerator(), tx, new MutableClock(Instant.parse("2026-11-11T09:00:00Z")));

    private final CustomerDetails joan = new CustomerDetails("Joan", "joan@example.com", null);

    @Test
    void placesAnOrderUsingTheProductPriceInOneTransaction() {
        db.products.put("p1", Fixtures.product("p1", "a", "120.00"));

        Order order = service.placeOrder(joan, List.of(new OrderLine("p1", 2)), "Matí");

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.items()).singleElement().satisfies(i -> {
            assertThat(i.unitPrice()).isEqualByComparingTo("120.00");
            assertThat(i.quantity()).isEqualTo(2);
        });
        assertThat(db.orders).containsKey(order.id());
        assertThat(db.customers.values()).singleElement().satisfies(c -> assertThat(c.email()).isEqualTo("joan@example.com"));
        assertThat(tx.transactions()).isEqualTo(1);
    }

    @Test
    void reusesAndRefreshesAnExistingCustomer() {
        db.products.put("p1", Fixtures.product("p1", "a", "10"));
        db.customers.put("cu1", Fixtures.customer("cu1", "joan@example.com"));

        Order order = service.placeOrder(new CustomerDetails("Joan Nou", "joan@example.com", "600111222"),
                List.of(new OrderLine("p1", 1)), null);

        assertThat(order.customer().id()).isEqualTo("cu1");
        assertThat(db.customers).hasSize(1);
        assertThat(db.customers.get("cu1").phone()).isEqualTo("600111222");
        assertThat(db.customers.get("cu1").name()).isEqualTo("Joan Nou");
    }

    @Test
    void rejectsUnknownProductsWithoutWritingAnything() {
        assertThatThrownBy(() -> service.placeOrder(joan, List.of(new OrderLine("missing", 1)), null))
                .isInstanceOf(UnknownProductsException.class);
        assertThat(db.orders).isEmpty();
        assertThat(db.customers).isEmpty();
    }

    @Test
    void changesTheStatusOfAnOrder() {
        db.products.put("p1", Fixtures.product("p1", "a", "10"));
        Order order = service.placeOrder(joan, List.of(new OrderLine("p1", 1)), null);

        Order confirmed = service.changeOrderStatus(order.id(), OrderStatus.CONFIRMED);

        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(service.listOrders(OrderStatus.CONFIRMED, new PageRequest(1, 20)).total()).isEqualTo(1);
        assertThat(service.listOrders(OrderStatus.PENDING, new PageRequest(1, 20)).total()).isZero();
        assertThatThrownBy(() -> service.changeOrderStatus("nope", OrderStatus.CANCELLED))
                .isInstanceOf(NotFoundException.class);
    }
}
