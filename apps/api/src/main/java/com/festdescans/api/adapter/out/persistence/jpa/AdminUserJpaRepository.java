package com.festdescans.api.adapter.out.persistence.jpa;

import com.festdescans.api.adapter.out.persistence.entity.AdminUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserJpaRepository extends JpaRepository<AdminUserEntity, String> {

    Optional<AdminUserEntity> findByEmail(String email);
}
