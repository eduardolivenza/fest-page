package com.festdescans.api.adapter.out.persistence;

import com.festdescans.api.adapter.out.persistence.entity.ProductEntity;
import com.festdescans.api.adapter.out.persistence.entity.ProductImageEntity;
import com.festdescans.api.adapter.out.persistence.entity.ProductSizeEntity;
import com.festdescans.api.adapter.out.persistence.jpa.OrderItemJpaRepository;
import com.festdescans.api.adapter.out.persistence.jpa.ProductJpaRepository;
import com.festdescans.api.application.port.out.ProductRepository;
import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductFilter;
import com.festdescans.api.domain.product.ProductImage;
import com.festdescans.api.domain.product.ProductInUseException;
import com.festdescans.api.domain.product.ProductSize;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
class ProductPersistenceAdapter implements ProductRepository {

    private static final Sort CATALOG_ORDER = Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("createdAt"));
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"));

    private final ProductJpaRepository products;
    private final OrderItemJpaRepository orderItems;
    private final EntityManager em;

    ProductPersistenceAdapter(ProductJpaRepository products, OrderItemJpaRepository orderItems, EntityManager em) {
        this.products = products;
        this.orderItems = orderItems;
        this.em = em;
    }

    @Override
    public Page<Product> findCatalogPage(ProductFilter filter, PageRequest page) {
        Specification<ProductEntity> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (filter.category() != null) {
                where.add(cb.equal(root.get("category"), filter.category()));
            }
            if (filter.featuredOnly()) {
                where.add(cb.isTrue(root.get("featured")));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        return EntityMapper.page(products.findAll(spec, pageable(page, CATALOG_ORDER)), page.page(),
                EntityMapper::product);
    }

    @Override
    public Page<Product> findPage(PageRequest page) {
        return EntityMapper.page(products.findAll(pageable(page, NEWEST_FIRST)), page.page(), EntityMapper::product);
    }

    @Override
    public Optional<Product> findById(String id) {
        return products.findById(id).map(EntityMapper::product);
    }

    @Override
    public Optional<Product> findByIdOrSlug(String idOrSlug) {
        return products.findFirstByIdOrSlug(idOrSlug, idOrSlug).map(EntityMapper::product);
    }

    @Override
    public Optional<Product> findBySlug(String slug) {
        return products.findBySlug(slug).map(EntityMapper::product);
    }

    @Override
    public List<Product> findAllById(Collection<String> ids) {
        return products.findAllById(ids).stream().map(EntityMapper::product).toList();
    }

    @Override
    @Transactional
    public Product save(Product product) {
        ProductEntity entity = products.findById(product.id()).orElse(null);
        boolean isNew = entity == null;
        if (isNew) {
            entity = new ProductEntity(product.id());
            entity.setCreatedAt(EntityMapper.utc(product.createdAt()));
        }
        entity.setName(product.name());
        entity.setSlug(product.slug());
        entity.setDescription(product.description());
        entity.setPrice(product.price());
        entity.setCategory(product.category());
        entity.setFeatured(product.featured());
        entity.setUpdatedAt(EntityMapper.utc(product.updatedAt()));
        syncImages(entity, product.images());
        syncSizes(entity, product.sizes());
        if (isNew) {
            em.persist(entity);
        }
        em.flush();
        return EntityMapper.product(entity);
    }

    @Override
    @Transactional
    public boolean deleteById(String id) {
        Optional<ProductEntity> entity = products.findById(id);
        if (entity.isEmpty()) {
            return false;
        }
        // Order items keep a foreign key to the product (ON DELETE RESTRICT).
        if (orderItems.existsByProduct_Id(id)) {
            throw new ProductInUseException(id);
        }
        try {
            products.delete(entity.get());
            products.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ProductInUseException(id);
        }
        return true;
    }

    private static org.springframework.data.domain.PageRequest pageable(PageRequest page, Sort sort) {
        return org.springframework.data.domain.PageRequest.of(page.page() - 1, page.pageSize(), sort);
    }

    private static void syncImages(ProductEntity entity, List<ProductImage> images) {
        Map<String, ProductImageEntity> existing = byId(entity.getImages(), ProductImageEntity::getId);
        entity.getImages().removeIf(e -> images.stream().noneMatch(i -> i.id().equals(e.getId())));
        for (ProductImage image : images) {
            ProductImageEntity e = existing.get(image.id());
            if (e == null) {
                e = new ProductImageEntity(image.id(), entity);
                entity.getImages().add(e);
            }
            e.setUrl(image.url());
            e.setAlt(image.alt());
            e.setPosition(image.position());
        }
    }

    private static void syncSizes(ProductEntity entity, List<ProductSize> sizes) {
        Map<String, ProductSizeEntity> existing = byId(entity.getSizes(), ProductSizeEntity::getId);
        entity.getSizes().removeIf(e -> sizes.stream().noneMatch(s -> s.id().equals(e.getId())));
        for (ProductSize size : sizes) {
            ProductSizeEntity e = existing.get(size.id());
            if (e == null) {
                e = new ProductSizeEntity(size.id(), entity);
                entity.getSizes().add(e);
            }
            e.setWidth(size.width());
            e.setLength(size.length());
            e.setPrice(size.price());
        }
    }

    private static <T> Map<String, T> byId(List<T> entities, Function<T, String> id) {
        return entities.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
