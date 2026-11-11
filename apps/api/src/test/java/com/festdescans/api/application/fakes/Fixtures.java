package com.festdescans.api.application.fakes;

import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.order.Customer;
import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductCategory;
import com.festdescans.api.domain.product.ProductImage;
import com.festdescans.api.domain.product.ProductSize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Ready-made domain objects for tests. */
public final class Fixtures {

    public static final Instant T0 = Instant.parse("2026-11-01T10:00:00Z");

    private Fixtures() {
    }

    public static Product product(String id, String slug, String price) {
        return new Product(id, "Product " + id, slug, null, new BigDecimal(price), ProductCategory.MATTRESS, false,
                List.of(), List.of(), T0, T0);
    }

    public static Product product(String id, ProductCategory category, boolean featured, Instant createdAt) {
        return new Product(id, "Product " + id, id, null, new BigDecimal("100.00"), category, featured,
                List.of(), List.of(), createdAt, createdAt);
    }

    public static Product withImagesAndSizes(Product p) {
        return new Product(p.id(), p.name(), p.slug(), p.description(), p.price(), p.category(), p.featured(),
                List.of(new ProductImage("img-2", "/b.jpg", null, 2, p.id()),
                        new ProductImage("img-1", "/a.jpg", "front", 1, p.id())),
                List.of(new ProductSize("s-2", 150, 190, new BigDecimal("599.90"), p.id()),
                        new ProductSize("s-1", 90, 190, new BigDecimal("499.90"), p.id())),
                p.createdAt(), p.updatedAt());
    }

    public static Customer customer(String id, String email) {
        return new Customer(id, "Customer " + id, email, null, T0, T0);
    }

    public static ContactRequest contact(String id, boolean read, Instant createdAt) {
        return new ContactRequest(id, "Maria", "maria@example.com", null, "Voldria informació.", read, createdAt);
    }
}
