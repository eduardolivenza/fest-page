package com.festdescans.api.adapter.in.web.product;

import com.festdescans.api.adapter.in.web.common.PageResponse;
import com.festdescans.api.adapter.in.web.error.ErrorResponse;
import com.festdescans.api.application.port.in.BrowseProductsUseCase;
import com.festdescans.api.domain.product.ProductCategory;
import com.festdescans.api.domain.product.ProductFilter;
import com.festdescans.api.domain.shared.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public catalogue. */
@RestController
@RequestMapping("/api/products")
class ProductController {

    static final int MAX_PAGE_SIZE = 50;

    private final BrowseProductsUseCase products;

    ProductController(BrowseProductsUseCase products) {
        this.products = products;
    }

    /** Featured first, then newest; each product with its first image only. */
    @GetMapping
    PageResponse<ProductResponse> list(
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String featured,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize) {
        ProductFilter filter = new ProductFilter(category, "true".equals(featured));
        return PageResponse.of(products.listProducts(filter, PageRequest.of(page, pageSize, MAX_PAGE_SIZE)),
                ProductResponse::from);
    }

    @GetMapping("/{idOrSlug}")
    ResponseEntity<?> get(@PathVariable String idOrSlug) {
        return products.findProduct(idOrSlug)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(ProductResponse.from(p)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Product not found")));
    }
}
