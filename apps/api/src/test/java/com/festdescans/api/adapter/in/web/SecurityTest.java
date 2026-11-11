package com.festdescans.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class SecurityTest extends WebTestBase {

    /**
     * Signed by the Node api (@fastify/jwt) with {@link WebTestBase#JWT_SECRET}: claims sub=admin-1,
     * email=admin@festdescans.com, expires in 2126. Sessions opened before the migration must stay valid.
     */
    private static final String NODE_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
            + "eyJzdWIiOiJhZG1pbi0xIiwiZW1haWwiOiJhZG1pbkBmZXN0ZGVzY2Fucy5jb20iLCJpYXQiOjE3OTQ0Mjc0ODIsImV4cCI6NDk0ODAyNzQ4Mn0."
            + "Pn6dPKu4difQvO61BQ5hJuylh77r89kLRtbC21MjUtQ";

    /** Same, but already expired. */
    private static final String EXPIRED_NODE_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
            + "eyJzdWIiOiJhZG1pbi0xIiwiZW1haWwiOiJhZG1pbkBmZXN0ZGVzY2Fucy5jb20iLCJpYXQiOjE3OTQ0Mjc0ODIsImV4cCI6MTc5NDQyNzQ3Mn0."
            + "CnMevY9MTyGe1ilrptBh-hIsLUiKrDCTST9TQSWHrS0";

    @ParameterizedTest(name = "{0} {1} -> 401 without a token")
    @CsvSource({
            "GET, /api/admin/contacts",
            "PATCH, /api/admin/contacts/c1/read",
            "DELETE, /api/admin/contacts/c1",
            "GET, /api/admin/products",
            "POST, /api/admin/products",
            "GET, /api/admin/orders",
            "PATCH, /api/admin/orders/o1/status",
            "GET, /api/admin/auth/me",
    })
    void adminRoutesRequireAToken(String method, String url) {
        assertThat(mvc.method(HttpMethod.valueOf(method)).uri(url)).hasStatus(401).bodyJson()
                .isStrictlyEqualTo("{\"message\":\"Unauthorized\"}");
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        assertThat(mvc.get().uri("/api/admin/contacts")
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.invalid")).hasStatus(401);
    }

    @Test
    void acceptsTokensIssuedByTheNodeApi() {
        assertThat(mvc.get().uri("/api/admin/contacts").header("Authorization", "Bearer " + NODE_TOKEN))
                .hasStatusOk();
    }

    @Test
    void rejectsExpiredTokens() {
        assertThat(mvc.get().uri("/api/admin/contacts").header("Authorization", "Bearer " + EXPIRED_NODE_TOKEN))
                .hasStatus(401);
    }

    @Test
    void rejectsUnsignedTokens() {
        String none = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0."
                + "eyJzdWIiOiJhZG1pbi0xIiwiZXhwIjo0OTQ4MDI3NDgyfQ.";
        assertThat(mvc.get().uri("/api/admin/contacts").header("Authorization", "Bearer " + none)).hasStatus(401);
    }

    @Test
    void loginIgnoresAStaleToken() {
        assertThat(mvc.post().uri("/api/admin/auth/login").header("Authorization", "Bearer " + EXPIRED_NODE_TOKEN)
                .contentType("application/json").content("{\"email\":\"x@y.com\",\"password\":\"p\"}"))
                .hasStatus(401).bodyJson().isStrictlyEqualTo("{\"message\":\"Invalid credentials\"}");
    }

    @Test
    void allowsCorsFromTheFrontends() {
        assertThat(mvc.options().uri("/api/admin/products")
                .header("Origin", "https://admin.festdescans.com")
                .header("Access-Control-Request-Method", "DELETE")
                .header("Access-Control-Request-Headers", "authorization"))
                .hasStatusOk()
                .hasHeader("Access-Control-Allow-Origin", "https://admin.festdescans.com")
                .hasHeader("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void refusesCorsFromOtherOrigins() {
        assertThat(mvc.options().uri("/api/products")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
                .hasStatus(403)
                .doesNotContainHeader("Access-Control-Allow-Origin");
    }
}
