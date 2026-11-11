package com.festdescans.api;

import com.festdescans.api.adapter.out.turnstile.TurnstileCaptchaVerifier;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** @param secretKey env TURNSTILE_SECRET_KEY; empty disables the check (local dev). */
@ConfigurationProperties("festpage.turnstile")
record TurnstileProperties(String secretKey, URI verifyUrl, @DefaultValue("5s") Duration timeout) {

    TurnstileProperties {
        verifyUrl = verifyUrl == null ? TurnstileCaptchaVerifier.CLOUDFLARE_VERIFY_URL : verifyUrl;
    }
}
