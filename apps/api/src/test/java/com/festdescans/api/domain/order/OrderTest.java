package com.festdescans.api.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.domain.shared.InvalidDataException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
    private final Customer customer = new Customer("cu1", "Joan", "joan@example.com", null, T0, T0);
    private final AtomicInteger seq = new AtomicInteger();

    @Test
    void placesAPendingOrderPricedWithTheCurrentProductPrice() {
        ProductSummary p1 = new ProductSummary("p1", "Matalàs", new BigDecimal("120.00"));

        Order order = Order.place("o1", customer, List.of(new OrderLine("p1", 2)), Map.of("p1", p1), "Urgent",
                () -> "i" + seq.incrementAndGet(), T0);

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.items()).singleElement().satisfies(item -> {
            assertThat(item.orderId()).isEqualTo("o1");
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.unitPrice()).isEqualByComparingTo("120.00");
        });
    }

    @Test
    void rejectsEmptyOrdersAndNonPositiveQuantities() {
        assertThatThrownBy(() -> Order.place("o1", customer, List.of(), Map.of(), null, () -> "i", T0))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new OrderLine("p1", 0)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void refreshingACustomerKeepsItsEmailAndCreationDate() {
        Instant later = T0.plusSeconds(60);
        Customer refreshed = customer.refresh(new CustomerDetails("Joan B", "other@example.com", "600"), later);

        assertThat(refreshed.email()).isEqualTo("joan@example.com");
        assertThat(refreshed.name()).isEqualTo("Joan B");
        assertThat(refreshed.phone()).isEqualTo("600");
        assertThat(refreshed.createdAt()).isEqualTo(T0);
        assertThat(refreshed.updatedAt()).isEqualTo(later);
    }
}
