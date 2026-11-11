package com.festdescans.api.domain.product;

import com.festdescans.api.domain.shared.InvalidDataException;
import com.festdescans.api.domain.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * A catalogue product. Images come ordered by position and sizes by width then length.
 * The canonical constructor only normalises: it accepts any row already in the database.
 * Business rules apply when a product is created or changed.
 */
public record Product(
        String id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        ProductCategory category,
        boolean featured,
        List<ProductImage> images,
        List<ProductSize> sizes,
        Instant createdAt,
        Instant updatedAt) {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9-]+$");

    public Product {
        Objects.requireNonNull(id, "id");
        images = images == null ? List.of()
                : images.stream().sorted(Comparator.comparingInt(ProductImage::position)).toList();
        sizes = sizes == null ? List.of()
                : sizes.stream()
                        .sorted(Comparator.comparingInt(ProductSize::width).thenComparingInt(ProductSize::length))
                        .toList();
    }

    public static Product create(ProductDraft draft, String id, Supplier<String> ids, Instant now) {
        List<ProductSize> sizes = toSizes(draft.sizes(), id, ids);
        return new Product(id, requireName(draft.name()), requireSlug(draft.slug()), draft.description(),
                Money.requirePositivePrice(draft.price()), Objects.requireNonNull(draft.category(), "category"),
                draft.featured(), List.of(), sizes, now, now);
    }

    public Product update(ProductChanges changes, Supplier<String> ids, Instant now) {
        return new Product(
                id,
                changes.name() != null ? requireName(changes.name()) : name,
                changes.slug() != null ? requireSlug(changes.slug()) : slug,
                changes.description() != null ? changes.description() : description,
                changes.price() != null ? Money.requirePositivePrice(changes.price()) : price,
                changes.category() != null ? changes.category() : category,
                changes.featured() != null ? changes.featured() : featured,
                images,
                changes.sizes() != null ? toSizes(changes.sizes(), id, ids) : sizes,
                createdAt,
                now);
    }

    /** Same product with only its first image (the public listing shows one thumbnail). */
    public Product withFirstImageOnly() {
        return images.size() <= 1 ? this
                : new Product(id, name, slug, description, price, category, featured, images.subList(0, 1), sizes,
                        createdAt, updatedAt);
    }

    private static List<ProductSize> toSizes(List<NewProductSize> sizes, String productId, Supplier<String> ids) {
        if (sizes == null) {
            return List.of();
        }
        return sizes.stream()
                .map(s -> new ProductSize(ids.get(), s.width(), s.length(), s.price(), productId))
                .toList();
    }

    private static String requireName(String name) {
        if (name == null || name.length() < 2 || name.length() > 200) {
            throw new InvalidDataException("name must have 2 to 200 characters");
        }
        return name;
    }

    private static String requireSlug(String slug) {
        if (slug == null || !SLUG.matcher(slug).matches()) {
            throw new InvalidDataException("slug may only contain a-z, 0-9 and -");
        }
        return slug;
    }
}
