package com.festdescans.api.adapter.out.persistence.jpa;

import com.festdescans.api.adapter.out.persistence.entity.ContactRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRequestJpaRepository extends JpaRepository<ContactRequestEntity, String> {

    Page<ContactRequestEntity> findByRead(boolean read, Pageable pageable);
}
