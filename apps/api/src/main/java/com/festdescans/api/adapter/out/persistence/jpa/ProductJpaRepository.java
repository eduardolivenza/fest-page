package com.festdescans.api.adapter.out.persistence.jpa;

import com.festdescans.api.adapter.out.persistence.entity.ProductEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, String>, JpaSpecificationExecutor<ProductEntity> {

    Optional<ProductEntity> findBySlug(String slug);

    Optional<ProductEntity> findFirstByIdOrSlug(String id, String slug);
}
