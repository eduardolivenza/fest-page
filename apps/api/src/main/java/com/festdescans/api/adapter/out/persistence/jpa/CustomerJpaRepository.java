package com.festdescans.api.adapter.out.persistence.jpa;

import com.festdescans.api.adapter.out.persistence.entity.CustomerEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, String> {

    Optional<CustomerEntity> findByEmail(String email);
}
