package com.festdescans.api.application.service;

import com.festdescans.api.application.port.in.BrowseProductsUseCase;
import com.festdescans.api.application.port.in.ManageProductsUseCase;
import com.festdescans.api.application.port.out.IdGenerator;
import com.festdescans.api.application.port.out.ProductRepository;
import com.festdescans.api.application.port.out.TransactionRunner;
import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductChanges;
import com.festdescans.api.domain.product.ProductDraft;
import com.festdescans.api.domain.product.ProductFilter;
import com.festdescans.api.domain.product.SlugAlreadyUsedException;
import com.festdescans.api.domain.shared.NotFoundException;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import java.time.Clock;
import java.util.Optional;

public final class ProductService implements BrowseProductsUseCase, ManageProductsUseCase {

    private final ProductRepository products;
    private final IdGenerator ids;
    private final TransactionRunner tx;
    private final Clock clock;

    public ProductService(ProductRepository products, IdGenerator ids, TransactionRunner tx, Clock clock) {
        this.products = products;
        this.ids = ids;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public Page<Product> listProducts(ProductFilter filter, PageRequest page) {
        // The public listing only needs a thumbnail.
        return products.findCatalogPage(filter, page).map(Product::withFirstImageOnly);
    }

    @Override
    public Optional<Product> findProduct(String idOrSlug) {
        return products.findByIdOrSlug(idOrSlug);
    }

    @Override
    public Page<Product> listAllProducts(PageRequest page) {
        return products.findPage(page);
    }

    @Override
    public Optional<Product> getProduct(String id) {
        return products.findById(id);
    }

    @Override
    public Product createProduct(ProductDraft draft) {
        return tx.inTransaction(() -> {
            requireFreeSlug(draft.slug(), null);
            Product product = Product.create(draft, ids.newId(), ids::newId, Timestamps.now(clock));
            return products.save(product);
        });
    }

    @Override
    public Product updateProduct(String id, ProductChanges changes) {
        return tx.inTransaction(() -> {
            Product current = products.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
            if (changes.slug() != null) {
                requireFreeSlug(changes.slug(), id);
            }
            return products.save(current.update(changes, ids::newId, Timestamps.now(clock)));
        });
    }

    @Override
    public void deleteProduct(String id) {
        if (!products.deleteById(id)) {
            throw new NotFoundException("Product", id);
        }
    }

    private void requireFreeSlug(String slug, String ownerId) {
        products.findBySlug(slug)
                .filter(other -> !other.id().equals(ownerId))
                .ifPresent(other -> {
                    throw new SlugAlreadyUsedException(slug);
                });
    }
}
