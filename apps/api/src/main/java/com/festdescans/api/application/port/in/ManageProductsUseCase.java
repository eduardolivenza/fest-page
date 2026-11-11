package com.festdescans.api.application.port.in;

import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductChanges;
import com.festdescans.api.domain.product.ProductDraft;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.util.Optional;

/** Back-office product management. */
public interface ManageProductsUseCase {

    /** Newest first. */
    Page<Product> listAllProducts(PageRequest page);

    Optional<Product> getProduct(String id);

    /** @throws com.festdescans.api.domain.product.SlugAlreadyUsedException if the slug is taken */
    Product createProduct(ProductDraft draft);

    /**
     * @throws com.festdescans.api.domain.shared.NotFoundException if there is no such product
     * @throws com.festdescans.api.domain.product.SlugAlreadyUsedException if the new slug is taken
     */
    Product updateProduct(String id, ProductChanges changes);

    /**
     * @throws com.festdescans.api.domain.shared.NotFoundException if there is no such product
     * @throws com.festdescans.api.domain.product.ProductInUseException if orders refer to it
     */
    void deleteProduct(String id);
}
