package com.festdescans.api.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.domain.shared.InvalidDataException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-11-02T10:00:00Z");

    private final AtomicInteger seq = new AtomicInteger();
    private final Supplier<String> ids = () -> "s" + seq.incrementAndGet();

    private ProductDraft draft(String slug, String price, List<NewProductSize> sizes) {
        return new ProductDraft("Matalàs", slug, null, new BigDecimal(price), ProductCategory.MATTRESS, false, sizes);
    }

    @Test
    void createsAProductWithSortedSizesAndTwoDecimalPrices() {
        Product p = Product.create(draft("matalas", "499.9",
                List.of(new NewProductSize(150, 190, new BigDecimal("599")),
                        new NewProductSize(90, 200, new BigDecimal("499.90")),
                        new NewProductSize(90, 190, new BigDecimal("489.90")))),
                "p1", ids, T0);

        assertThat(p.price()).isEqualByComparingTo("499.90").hasScaleOf(2);
        assertThat(p.sizes()).extracting(ProductSize::width, ProductSize::length)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(90, 190), org.assertj.core.groups.Tuple.tuple(90, 200),
                        org.assertj.core.groups.Tuple.tuple(150, 190));
        assertThat(p.sizes()).allSatisfy(s -> assertThat(s.productId()).isEqualTo("p1"));
        assertThat(p.createdAt()).isEqualTo(T0).isEqualTo(p.updatedAt());
    }

    @Test
    void rejectsInvalidSlugsAndPrices() {
        assertThatThrownBy(() -> Product.create(draft("Not A Slug", "10", null), "p1", ids, T0))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> Product.create(draft("ok", "0", null), "p1", ids, T0))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> new NewProductSize(0, 190, BigDecimal.TEN)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void updateChangesOnlyTheGivenFields() {
        Product p = Product.create(draft("matalas", "100", List.of(new NewProductSize(90, 190, BigDecimal.TEN))),
                "p1", ids, T0);

        Product updated = p.update(new ProductChanges(null, null, null, new BigDecimal("120"), null, true, null), ids, T1);

        assertThat(updated.name()).isEqualTo("Matalàs");
        assertThat(updated.price()).isEqualByComparingTo("120");
        assertThat(updated.featured()).isTrue();
        assertThat(updated.sizes()).isEqualTo(p.sizes());
        assertThat(updated.createdAt()).isEqualTo(T0);
        assertThat(updated.updatedAt()).isEqualTo(T1);
    }

    @Test
    void updateWithSizesReplacesThemAndAnEmptyListRemovesThem() {
        Product p = Product.create(draft("matalas", "100", List.of(new NewProductSize(90, 190, BigDecimal.TEN))),
                "p1", ids, T0);

        Product replaced = p.update(new ProductChanges(null, null, null, null, null, null,
                List.of(new NewProductSize(135, 190, BigDecimal.ONE))), ids, T1);
        Product cleared = p.update(new ProductChanges(null, null, null, null, null, null, List.of()), ids, T1);

        assertThat(replaced.sizes()).singleElement().satisfies(s -> {
            assertThat(s.width()).isEqualTo(135);
            assertThat(s.id()).isNotEqualTo(p.sizes().getFirst().id());
        });
        assertThat(cleared.sizes()).isEmpty();
    }

    @Test
    void loadingAnExistingRowNeverValidates() {
        // Rows already in the database are accepted as they are (e.g. a legacy price of 0).
        Product legacy = new Product("p1", "X", "Legacy Slug", null, BigDecimal.ZERO, ProductCategory.BED, false,
                null, null, T0, T0);
        assertThat(legacy.images()).isEmpty();
        assertThat(legacy.sizes()).isEmpty();
    }

    @Test
    void keepsOnlyTheFirstImageByPosition() {
        Product p = new Product("p1", "X", "x", null, BigDecimal.ONE, ProductCategory.BED, false,
                List.of(new ProductImage("b", "/b", null, 2, "p1"), new ProductImage("a", "/a", null, 1, "p1")),
                List.of(), T0, T0);

        assertThat(p.withFirstImageOnly().images()).extracting(ProductImage::id).containsExactly("a");
    }
}
