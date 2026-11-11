package com.festdescans.api.application.service;

import static com.festdescans.api.application.fakes.Fixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.application.fakes.InMemoryDatabase;
import com.festdescans.api.application.port.in.InvalidCredentialsException;
import com.festdescans.api.domain.admin.AdminUser;
import org.junit.jupiter.api.Test;

class AdminAuthServiceTest {

    private final InMemoryDatabase db = new InMemoryDatabase();
    // A trivial verifier: the real Argon2 one is tested in adapter-out-password.
    private final AdminAuthService service =
            new AdminAuthService(db.adminUserRepository, (raw, hash) -> hash.equals("hash:" + raw));

    @Test
    void authenticatesWithTheRightPassword() {
        db.admins.put("a1", new AdminUser("a1", "admin@festdescans.com", "hash:right", "Admin", T0, T0));

        assertThat(service.authenticate("admin@festdescans.com", "right").id()).isEqualTo("a1");
        assertThatThrownBy(() -> service.authenticate("admin@festdescans.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.authenticate("unknown@festdescans.com", "right"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void neverPrintsThePasswordHash() {
        AdminUser admin = new AdminUser("a1", "admin@festdescans.com", "hash:secret", "Admin", T0, T0);
        assertThat(admin.toString()).doesNotContain("secret");
    }
}
