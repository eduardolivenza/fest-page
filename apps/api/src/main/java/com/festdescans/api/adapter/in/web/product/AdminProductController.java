package com.festdescans.api.adapter.in.web.product;

import com.festdescans.api.adapter.in.web.common.PageResponse;
import com.festdescans.api.adapter.in.web.common.Validation.OnCreate;
import com.festdescans.api.application.port.in.ManageProductsUseCase;
import com.festdescans.api.domain.shared.NotFoundException;
import com.festdescans.api.domain.shared.PageRequest;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
class AdminProductController {

    static final int MAX_PAGE_SIZE = 100;

    private final ManageProductsUseCase products;

    AdminProductController(ManageProductsUseCase products) {
        this.products = products;
    }

    @GetMapping
    PageResponse<ProductResponse> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.of(products.listAllProducts(PageRequest.of(page, pageSize, MAX_PAGE_SIZE)),
                ProductResponse::from);
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable String id) {
        return products.getProduct(id).map(ProductResponse::from)
                .orElseThrow(() -> new NotFoundException("Product", id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductResponse create(@RequestBody @Validated({Default.class, OnCreate.class}) ProductRequest body) {
        return ProductResponse.from(products.createProduct(body.toDraft()));
    }

    /** Partial update: omitted fields are kept; {@code sizes}, when present, replaces all sizes. */
    @PutMapping("/{id}")
    ProductResponse update(@PathVariable String id, @RequestBody @Valid ProductRequest body) {
        return ProductResponse.from(products.updateProduct(id, body.toChanges()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) {
        products.deleteProduct(id);
    }
}
