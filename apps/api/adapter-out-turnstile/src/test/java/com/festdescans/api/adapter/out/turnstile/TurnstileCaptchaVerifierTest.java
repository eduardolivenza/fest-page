package com.festdescans.api.adapter.out.turnstile;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against a local HTTP server standing in for Cloudflare's siteverify endpoint. */
class TurnstileCaptchaVerifierTest {

    private HttpServer server;
    private final List<Map<String, String>> requests = new ArrayList<>();
    private volatile String answer = "{\"success\":true}";
    private volatile long delayMillis;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/siteverify", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(parseForm(body));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] out = answer.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private TurnstileCaptchaVerifier verifier(String secret) {
        URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify");
        return new TurnstileCaptchaVerifier(secret, url, Duration.ofMillis(500));
    }

    @Test
    void passesWithoutCallingCloudflareWhenNoSecretIsConfigured() {
        assertThat(verifier("").verify(null, null)).isTrue();
        assertThat(verifier(null).verify("tok", null)).isTrue();
        assertThat(requests).isEmpty();
    }

    @Test
    void failsWithoutCallingCloudflareWhenTheTokenIsMissing() {
        assertThat(verifier("secret").verify(null, "203.0.113.7")).isFalse();
        assertThat(requests).isEmpty();
    }

    @Test
    void sendsSecretTokenAndVisitorIp() {
        assertThat(verifier("secret").verify("tok", "203.0.113.7")).isTrue();

        assertThat(requests).singleElement().isEqualTo(
                Map.of("secret", "secret", "response", "tok", "remoteip", "203.0.113.7"));
    }

    @Test
    void failsWhenCloudflareRejectsTheToken() {
        answer = "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}";
        assertThat(verifier("secret").verify("bad", null)).isFalse();
    }

    @Test
    void failsClosedOnGarbageOrTimeouts() {
        answer = "<html>oops</html>";
        assertThat(verifier("secret").verify("tok", null)).isFalse();

        answer = "{\"success\":\"true\"}";
        assertThat(verifier("secret").verify("tok", null)).as("success must be a JSON boolean").isFalse();

        answer = "{\"success\":true}";
        delayMillis = 2_000;
        assertThat(verifier("secret").verify("tok", null)).isFalse();
    }

    private static Map<String, String> parseForm(String body) {
        return Arrays.stream(body.split("&"))
                .map(kv -> kv.split("=", 2))
                .collect(Collectors.toMap(kv -> URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        kv -> URLDecoder.decode(kv[1], StandardCharsets.UTF_8)));
    }
}
