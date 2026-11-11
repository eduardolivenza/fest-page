package com.festdescans.api.adapter.in.web.product;

import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductCategory;
import com.festdescans.api.domain.product.ProductImage;
import com.festdescans.api.domain.product.ProductSize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Product as returned by public and admin endpoints (prices are JSON numbers). */
public record ProductResponse(
        String id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        ProductCategory category,
        boolean featured,
        List<Image> images,
        List<Size> sizes,
        Instant createdAt,
        Instant updatedAt) {

    public record Image(String id, String url, String alt, int position, String productId) {

        static Image from(ProductImage i) {
            return new Image(i.id(), i.url(), i.alt(), i.position(), i.productId());
        }
    }

    public record Size(String id, int width, int length, BigDecimal price, String productId) {

        static Size from(ProductSize s) {
            return new Size(s.id(), s.width(), s.length(), s.price(), s.productId());
        }
    }

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.id(), p.name(), p.slug(), p.description(), p.price(), p.category(), p.featured(),
                p.images().stream().map(Image::from).toList(), p.sizes().stream().map(Size::from).toList(),
                p.createdAt(), p.updatedAt());
    }
}
