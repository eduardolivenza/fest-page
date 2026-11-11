package com.festdescans.api.application.port.in;

import com.festdescans.api.domain.admin.AdminUser;
import java.util.Optional;

public interface AuthenticateAdminUseCase {

    /**
     * @throws InvalidCredentialsException for an unknown email or a wrong password (same error for
     *                                     both, so emails can't be probed)
     */
    AdminUser authenticate(String email, String password);

    Optional<AdminUser> findAdmin(String id);
}
