package com.festdescans.api.application.port.out;

import com.festdescans.api.domain.admin.AdminUser;
import java.util.Optional;

public interface AdminUserRepository {

    Optional<AdminUser> findByEmail(String email);

    Optional<AdminUser> findById(String id);
}
