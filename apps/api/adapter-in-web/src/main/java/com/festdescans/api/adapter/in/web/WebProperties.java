package com.festdescans.api.adapter.in.web;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param jwtSecret   HMAC-SHA256 key for admin tokens (env JWT_SECRET, at least 32 bytes)
 * @param tokenTtl    lifetime of an admin token
 * @param corsOrigins frontends allowed to call the api from the browser (env WEB_URL, ADMIN_URL)
 */
@ConfigurationProperties("festpage.web")
public record WebProperties(
        String jwtSecret,
        @DefaultValue("8h") Duration tokenTtl,
        @DefaultValue({"http://localhost:3000", "http://localhost:3002"}) List<String> corsOrigins) {
}
