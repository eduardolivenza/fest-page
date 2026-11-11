package com.festdescans.api.application.port.out;

import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductFilter;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    /** Featured first, then newest first. */
    Page<Product> findCatalogPage(ProductFilter filter, PageRequest page);

    /** Newest first. */
    Page<Product> findPage(PageRequest page);

    Optional<Product> findById(String id);

    /** Matches either the id or the slug. */
    Optional<Product> findByIdOrSlug(String idOrSlug);

    Optional<Product> findBySlug(String slug);

    List<Product> findAllById(Collection<String> ids);

    /** Inserts or updates the product and replaces its images and sizes with the given ones. */
    Product save(Product product);

    /**
     * @return false if there was no such product
     * @throws com.festdescans.api.domain.product.ProductInUseException if order items refer to it
     */
    boolean deleteById(String id);
}
