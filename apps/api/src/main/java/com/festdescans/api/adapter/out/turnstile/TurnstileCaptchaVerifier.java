package com.festdescans.api.adapter.out.turnstile;

import com.festdescans.api.application.port.out.CaptchaVerifier;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies Cloudflare Turnstile tokens with the siteverify API.
 *
 * <p>When no secret key is configured (local dev, or before the SealedSecret exists) every check passes.
 * Any network or parsing error fails the check (fail closed).
 */
public final class TurnstileCaptchaVerifier implements CaptchaVerifier {

    public static final URI CLOUDFLARE_VERIFY_URL =
            URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify");

    private final String secretKey;
    private final URI verifyUrl;
    private final Duration timeout;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public TurnstileCaptchaVerifier(String secretKey, URI verifyUrl, Duration timeout) {
        this.secretKey = secretKey;
        this.verifyUrl = verifyUrl;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public boolean enabled() {
        return secretKey != null && !secretKey.isBlank();
    }

    @Override
    public boolean verify(String token, String remoteIp) {
        if (!enabled()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("secret", secretKey);
        form.put("response", token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            form.put("remoteip", remoteIp);
        }
        HttpRequest request = HttpRequest.newBuilder(verifyUrl)
                .timeout(timeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encode(form)))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = json.readTree(response.body());
            return body.path("success").asBoolean(false) && body.path("success").isBoolean();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String encode(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
