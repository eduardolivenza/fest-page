package com.festdescans.api.adapter.out.persistence;

import com.festdescans.api.adapter.out.persistence.entity.AdminUserEntity;
import com.festdescans.api.adapter.out.persistence.entity.ContactRequestEntity;
import com.festdescans.api.adapter.out.persistence.entity.CustomerEntity;
import com.festdescans.api.adapter.out.persistence.entity.OrderEntity;
import com.festdescans.api.adapter.out.persistence.entity.OrderItemEntity;
import com.festdescans.api.adapter.out.persistence.entity.ProductEntity;
import com.festdescans.api.domain.admin.AdminUser;
import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.order.Customer;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderItem;
import com.festdescans.api.domain.order.ProductSummary;
import com.festdescans.api.domain.product.Product;
import com.festdescans.api.domain.product.ProductImage;
import com.festdescans.api.domain.product.ProductSize;
import com.festdescans.api.domain.shared.Page;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;

/** Entity -> domain record conversions. Columns are TIMESTAMP without time zone holding UTC (as Prisma wrote them). */
final class EntityMapper {

    private EntityMapper() {
    }

    static Instant instant(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }

    static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static <E, D> Page<D> page(org.springframework.data.domain.Page<E> page, int pageNumber,
            Function<E, D> mapper) {
        return new Page<>(page.getContent().stream().map(mapper).toList(), page.getTotalElements(), pageNumber,
                page.getSize());
    }

    static Product product(ProductEntity e) {
        return new Product(
                e.getId(),
                e.getName(),
                e.getSlug(),
                e.getDescription(),
                e.getPrice(),
                e.getCategory(),
                e.isFeatured(),
                e.getImages().stream()
                        .map(i -> new ProductImage(i.getId(), i.getUrl(), i.getAlt(), i.getPosition(), e.getId()))
                        .toList(),
                e.getSizes().stream()
                        .map(s -> new ProductSize(s.getId(), s.getWidth(), s.getLength(), s.getPrice(), e.getId()))
                        .toList(),
                instant(e.getCreatedAt()),
                instant(e.getUpdatedAt()));
    }

    static Customer customer(CustomerEntity e) {
        return new Customer(e.getId(), e.getName(), e.getEmail(), e.getPhone(), instant(e.getCreatedAt()),
                instant(e.getUpdatedAt()));
    }

    static Order order(OrderEntity e) {
        return new Order(
                e.getId(),
                customer(e.getCustomer()),
                e.getItems().stream().map(EntityMapper::orderItem).toList(),
                e.getStatus(),
                e.getNotes(),
                instant(e.getCreatedAt()),
                instant(e.getUpdatedAt()));
    }

    private static OrderItem orderItem(OrderItemEntity e) {
        ProductEntity p = e.getProduct();
        return new OrderItem(e.getId(), e.getOrder().getId(), new ProductSummary(p.getId(), p.getName(), p.getPrice()),
                e.getQuantity(), e.getUnitPrice());
    }

    static ContactRequest contact(ContactRequestEntity e) {
        return new ContactRequest(e.getId(), e.getName(), e.getEmail(), e.getPhone(), e.getMessage(), e.isRead(),
                instant(e.getCreatedAt()));
    }

    static AdminUser admin(AdminUserEntity e) {
        return new AdminUser(e.getId(), e.getEmail(), e.getPasswordHash(), e.getName(), instant(e.getCreatedAt()),
                instant(e.getUpdatedAt()));
    }
}
