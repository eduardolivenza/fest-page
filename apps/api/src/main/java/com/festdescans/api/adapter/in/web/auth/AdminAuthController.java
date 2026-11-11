package com.festdescans.api.adapter.in.web.auth;

import com.festdescans.api.adapter.in.web.common.Validation;
import com.festdescans.api.adapter.in.web.error.ErrorResponse;
import com.festdescans.api.adapter.in.web.security.AdminTokens;
import com.festdescans.api.application.port.in.AuthenticateAdminUseCase;
import com.festdescans.api.domain.admin.AdminUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
class AdminAuthController {

    private final AuthenticateAdminUseCase auth;
    private final AdminTokens tokens;

    AdminAuthController(AuthenticateAdminUseCase auth, AdminTokens tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    record LoginRequest(@NotNull @Pattern(regexp = Validation.EMAIL) String email, @NotEmpty String password) {
    }

    record LoginResponse(String token, UserResponse user) {
    }

    record UserResponse(String id, String email, String name) {
    }

    record MeResponse(String id, String email, String name, Instant createdAt) {
    }

    /** Never says whether the email exists: invalid input, unknown user and wrong password look alike. */
    @PostMapping("/login")
    ResponseEntity<?> login(@RequestBody @Valid LoginRequest body, BindingResult validation) {
        if (validation.hasErrors()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid credentials"));
        }
        AdminUser admin = auth.authenticate(body.email(), body.password());
        return ResponseEntity.ok(new LoginResponse(tokens.issue(admin),
                new UserResponse(admin.id(), admin.email(), admin.name())));
    }

    @GetMapping("/me")
    ResponseEntity<?> me(@AuthenticationPrincipal Jwt token) {
        return auth.findAdmin(token.getSubject())
                .<ResponseEntity<?>>map(a -> ResponseEntity.ok(new MeResponse(a.id(), a.email(), a.name(), a.createdAt())))
                // The token outlived its user: make the admin app log in again.
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Unauthorized")));
    }
}
