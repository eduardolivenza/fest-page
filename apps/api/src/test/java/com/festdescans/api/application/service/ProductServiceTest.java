package com.festdescans.api.application.service;

import static com.festdescans.api.application.fakes.Fixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.application.fakes.DirectTransactionRunner;
import com.festdescans.api.application.fakes.Fixtures;
import com.festdescans.api.application.fakes.InMemoryDatabase;
import com.festdescans.api.application.fakes.MutableClock;
import com.festdescans.api.application.fakes.SequentialIdGenerator;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderItem;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.order.ProductSummary;
import com.festdescans.api.domain.product.NewProductSize;
import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductCategory;
import com.festdescans.api.domain.product.ProductChanges;
import com.festdescans.api.domain.product.ProductDraft;
import com.festdescans.api.domain.product.ProductFilter;
import com.festdescans.api.domain.product.ProductInUseException;
import com.festdescans.api.domain.product.SlugAlreadyUsedException;
import com.festdescans.api.domain.shared.NotFoundException;
import com.festdescans.api.domain.shared.PageRequest;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private final InMemoryDatabase db = new InMemoryDatabase();
    private final DirectTransactionRunner tx = new DirectTransactionRunner();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-11T09:00:00.123456Z"));
    private final ProductService service =
            new ProductService(db.productRepository, new SequentialIdGenerator(), tx, clock);

    @Test
    void catalogueListsFeaturedFirstThenNewestWithOneThumbnail() {
        db.products.put("old", Fixtures.product("old", ProductCategory.BED, false, T0));
        db.products.put("new", Fixtures.product("new", ProductCategory.BED, false, T0.plusSeconds(60)));
        db.products.put("feat", Fixtures.withImagesAndSizes(Fixtures.product("feat", ProductCategory.BED, true, T0)));

        var page = service.listProducts(ProductFilter.none(), new PageRequest(1, 12));

        assertThat(page.data()).extracting(Product::id).containsExactly("feat", "new", "old");
        assertThat(page.data().getFirst().images()).extracting(i -> i.id()).containsExactly("img-1");
        assertThat(page.data().getFirst().sizes()).hasSize(2);
    }

    @Test
    void catalogueFiltersByCategoryAndFeatured() {
        db.products.put("bed", Fixtures.product("bed", ProductCategory.BED, true, T0));
        db.products.put("pillow", Fixtures.product("pillow", ProductCategory.PILLOW, false, T0));

        assertThat(service.listProducts(new ProductFilter(ProductCategory.PILLOW, false), new PageRequest(1, 12)).data())
                .extracting(Product::id).containsExactly("pillow");
        assertThat(service.listProducts(new ProductFilter(null, true), new PageRequest(1, 12)).data())
                .extracting(Product::id).containsExactly("bed");
    }

    @Test
    void findsAProductByIdOrSlug() {
        db.products.put("p1", Fixtures.product("p1", "matalas-comfort", "10"));

        assertThat(service.findProduct("p1")).isPresent();
        assertThat(service.findProduct("matalas-comfort")).isPresent();
        assertThat(service.findProduct("nope")).isEmpty();
    }

    @Test
    void createsAProductWithMillisecondTimestamps() {
        Product created = service.createProduct(new ProductDraft("Coixí", "coixi", null, new BigDecimal("49.99"),
                ProductCategory.PILLOW, false, List.of(new NewProductSize(70, 40, new BigDecimal("49.99")))));

        assertThat(db.products).containsKey(created.id());
        assertThat(created.createdAt()).isEqualTo(Instant.parse("2026-11-11T09:00:00.123Z"));
        assertThat(created.sizes()).singleElement().satisfies(s -> assertThat(s.productId()).isEqualTo(created.id()));
        assertThat(tx.transactions()).isEqualTo(1);
    }

    @Test
    void rejectsADuplicateSlug() {
        db.products.put("p1", Fixtures.product("p1", "coixi", "10"));

        assertThatThrownBy(() -> service.createProduct(new ProductDraft("Coixí", "coixi", null, BigDecimal.TEN,
                ProductCategory.PILLOW, false, null))).isInstanceOf(SlugAlreadyUsedException.class);
    }

    @Test
    void updatesAProductKeepingItsOwnSlug() {
        db.products.put("p1", Fixtures.product("p1", "coixi", "10"));
        clock.advance(Duration.ofHours(1));

        Product updated = service.updateProduct("p1",
                new ProductChanges("Coixí nou", "coixi", null, null, null, null, null));

        assertThat(updated.name()).isEqualTo("Coixí nou");
        assertThat(updated.updatedAt()).isAfter(updated.createdAt());
        assertThat(db.products.get("p1").name()).isEqualTo("Coixí nou");
    }

    @Test
    void updatingAMissingProductIsNotFound() {
        assertThatThrownBy(() -> service.updateProduct("nope",
                new ProductChanges(null, null, null, null, null, null, null))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deletesAProductUnlessOrdersReferToIt() {
        db.products.put("p1", Fixtures.product("p1", "a", "10"));
        db.products.put("p2", Fixtures.product("p2", "b", "10"));
        db.customers.put("cu1", Fixtures.customer("cu1", "joan@example.com"));
        db.orders.put("o1", new Order("o1", db.customers.get("cu1"),
                List.of(new OrderItem("i1", "o1", new ProductSummary("p2", "b", BigDecimal.TEN), 1, BigDecimal.TEN)),
                OrderStatus.PENDING, null, T0, T0));

        service.deleteProduct("p1");

        assertThat(db.products).doesNotContainKey("p1");
        assertThatThrownBy(() -> service.deleteProduct("p2")).isInstanceOf(ProductInUseException.class);
        assertThatThrownBy(() -> service.deleteProduct("p1")).isInstanceOf(NotFoundException.class);
    }
}
