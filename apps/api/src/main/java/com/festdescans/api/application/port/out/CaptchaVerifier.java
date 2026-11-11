package com.festdescans.api.application.port.out;

/** Anti-bot check for public write endpoints (Cloudflare Turnstile). */
public interface CaptchaVerifier {

    /** @return true if the visitor passed, or if verification is not configured (local dev) */
    boolean verify(String token, String remoteIp);
}
