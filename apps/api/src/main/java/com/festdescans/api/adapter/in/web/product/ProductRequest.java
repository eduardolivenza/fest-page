package com.festdescans.api.adapter.in.web.product;

import com.festdescans.api.adapter.in.web.common.Validation.OnCreate;
import com.festdescans.api.domain.product.NewProductSize;
import com.festdescans.api.domain.product.ProductCategory;
import com.festdescans.api.domain.product.ProductChanges;
import com.festdescans.api.domain.product.ProductDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Body of POST (all required fields, group OnCreate) and PUT (partial update) /api/admin/products. */
public record ProductRequest(
        @NotNull(groups = OnCreate.class) @Size(min = 2, max = 200) String name,
        @NotNull(groups = OnCreate.class) @Pattern(regexp = "^[a-z0-9-]+$") String slug,
        String description,
        @NotNull(groups = OnCreate.class) @Positive BigDecimal price,
        @NotNull(groups = OnCreate.class) ProductCategory category,
        Boolean featured,
        List<@Valid @NotNull SizeRequest> sizes) {

    public record SizeRequest(
            @NotNull @Positive Integer width,
            @NotNull @Positive Integer length,
            @NotNull @Positive BigDecimal price) {

        NewProductSize toDomain() {
            return new NewProductSize(width, length, price);
        }
    }

    ProductDraft toDraft() {
        return new ProductDraft(name, slug, description, price, category, Boolean.TRUE.equals(featured), domainSizes());
    }

    ProductChanges toChanges() {
        return new ProductChanges(name, slug, description, price, category, featured, domainSizes());
    }

    private List<NewProductSize> domainSizes() {
        return sizes == null ? null : sizes.stream().map(SizeRequest::toDomain).toList();
    }
}
