package com.festdescans.api.adapter.out.persistence.jpa;

import com.festdescans.api.adapter.out.persistence.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemJpaRepository extends JpaRepository<OrderItemEntity, String> {

    boolean existsByProduct_Id(String productId);
}
