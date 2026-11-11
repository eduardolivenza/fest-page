package com.festdescans.api.adapter.out.persistence;

import com.festdescans.api.adapter.out.persistence.jpa.AdminUserJpaRepository;
import com.festdescans.api.application.port.out.AdminUserRepository;
import com.festdescans.api.domain.admin.AdminUser;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
class AdminUserPersistenceAdapter implements AdminUserRepository {

    private final AdminUserJpaRepository admins;

    AdminUserPersistenceAdapter(AdminUserJpaRepository admins) {
        this.admins = admins;
    }

    @Override
    public Optional<AdminUser> findByEmail(String email) {
        return admins.findByEmail(email).map(EntityMapper::admin);
    }

    @Override
    public Optional<AdminUser> findById(String id) {
        return admins.findById(id).map(EntityMapper::admin);
    }
}
