package com.festdescans.api.application.service;

import com.festdescans.api.application.port.in.AuthenticateAdminUseCase;
import com.festdescans.api.application.port.in.InvalidCredentialsException;
import com.festdescans.api.application.port.out.AdminUserRepository;
import com.festdescans.api.application.port.out.PasswordVerifier;
import com.festdescans.api.domain.admin.AdminUser;
import java.util.Optional;

public final class AdminAuthService implements AuthenticateAdminUseCase {

    private final AdminUserRepository admins;
    private final PasswordVerifier passwords;

    public AdminAuthService(AdminUserRepository admins, PasswordVerifier passwords) {
        this.admins = admins;
        this.passwords = passwords;
    }

    @Override
    public AdminUser authenticate(String email, String password) {
        return admins.findByEmail(email)
                .filter(admin -> passwords.matches(password, admin.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
    }

    @Override
    public Optional<AdminUser> findAdmin(String id) {
        return admins.findById(id);
    }
}
