package com.festdescans.api.adapter.out.persistence;

import com.festdescans.api.adapter.out.persistence.entity.CustomerEntity;
import com.festdescans.api.adapter.out.persistence.entity.OrderEntity;
import com.festdescans.api.adapter.out.persistence.entity.OrderItemEntity;
import com.festdescans.api.adapter.out.persistence.entity.ProductEntity;
import com.festdescans.api.adapter.out.persistence.jpa.OrderJpaRepository;
import com.festdescans.api.application.port.out.OrderRepository;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderItem;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.shared.Page;
import com.festdescans.api.domain.shared.PageRequest;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
class OrderPersistenceAdapter implements OrderRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"));

    private final OrderJpaRepository orders;
    private final EntityManager em;

    OrderPersistenceAdapter(OrderJpaRepository orders, EntityManager em) {
        this.orders = orders;
        this.em = em;
    }

    @Override
    public Page<Order> findPage(OrderStatus status, PageRequest page) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page.page() - 1, page.pageSize(),
                NEWEST_FIRST);
        var result = status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable);
        return EntityMapper.page(result, page.page(), EntityMapper::order);
    }

    @Override
    public Optional<Order> findById(String id) {
        return orders.findById(id).map(EntityMapper::order);
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderEntity entity = orders.findById(order.id()).orElse(null);
        if (entity == null) {
            entity = new OrderEntity(order.id(), em.getReference(CustomerEntity.class, order.customer().id()));
            entity.setCreatedAt(EntityMapper.utc(order.createdAt()));
            for (OrderItem item : order.items()) {
                entity.getItems().add(new OrderItemEntity(item.id(), entity,
                        em.getReference(ProductEntity.class, item.product().id()), item.quantity(), item.unitPrice()));
            }
            entity.setStatus(order.status());
            entity.setNotes(order.notes());
            entity.setUpdatedAt(EntityMapper.utc(order.updatedAt()));
            em.persist(entity);
            em.flush();
            // The customer and product references load lazily while mapping, inside this transaction.
            return EntityMapper.order(entity);
        }
        // Existing orders: items are immutable, only the status and notes change.
        entity.setStatus(order.status());
        entity.setNotes(order.notes());
        entity.setUpdatedAt(EntityMapper.utc(order.updatedAt()));
        em.flush();
        return EntityMapper.order(entity);
    }
}
