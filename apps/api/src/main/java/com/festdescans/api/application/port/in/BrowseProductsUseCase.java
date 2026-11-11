package com.festdescans.api.application.port.in;

import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductFilter;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.util.Optional;

/** Public catalogue. */
public interface BrowseProductsUseCase {

    /** Featured first, then newest first. */
    Page<Product> listProducts(ProductFilter filter, PageRequest page);

    Optional<Product> findProduct(String idOrSlug);
}
