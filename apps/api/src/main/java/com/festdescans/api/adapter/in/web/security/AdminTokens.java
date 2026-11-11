package com.festdescans.api.adapter.in.web.security;

import com.festdescans.api.adapter.in.web.WebProperties;
import com.festdescans.api.domain.admin.AdminUser;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Issues admin session tokens: an HTTP concern, so it lives in the web adapter, not in the application. */
@Component
public class AdminTokens {

    private final JwtEncoder encoder;
    private final WebProperties properties;
    private final Clock clock;

    AdminTokens(JwtEncoder encoder, WebProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public String issue(AdminUser admin) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(admin.id())
                .claim("email", admin.email())
                .issuedAt(now)
                .expiresAt(now.plus(properties.tokenTtl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
