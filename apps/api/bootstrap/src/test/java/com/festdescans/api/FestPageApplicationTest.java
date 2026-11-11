package com.festdescans.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.festdescans.api.adapter.out.persistence.EmbeddedPostgresDatabase;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The whole application (all adapters, real wiring, Flyway) against a real PostgreSQL, seeded with
 * apps/api/dev/seed.sql. One test walks the main flows of the public site and the back office.
 */
@SpringBootTest(properties = "festpage.web.jwt-secret=end-to-end-test-secret-0123456789abcdef")
@AutoConfigureMockMvc
class FestPageApplicationTest {

    private static final EmbeddedPostgresDatabase DB = EmbeddedPostgresDatabase.create();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::jdbcUrl);
        registry.add("spring.datasource.username", DB::username);
        registry.add("spring.datasource.password", DB::password);
    }

    @Autowired
    MockMvcTester mvc;
    @Autowired
    JdbcClient jdbc;

    @Test
    void publicSiteAndBackOfficeWorkEndToEnd() throws Exception {
        jdbc.sql(Files.readString(Path.of("../dev/seed.sql"))).update();

        // Public catalogue (seeded products, featured first).
        MvcTestResult catalogue = mvc.get().uri("/api/products").exchange();
        assertThat(catalogue).hasStatusOk().bodyJson().extractingPath("$.total").isEqualTo(6);
        assertThat(catalogue).bodyJson().extractingPath("$.data[0].featured").isEqualTo(true);
        assertThat(mvc.get().uri("/api/products?category=PILLOW")).bodyJson().extractingPath("$.total").isEqualTo(2);
        assertThat(mvc.get().uri("/api/products/colchon-comfort-plus")).hasStatusOk().bodyJson()
                .extractingPath("$.price").isEqualTo(649.99);

        // Back-office login with the seeded admin (Argon2 hash in seed.sql).
        MvcTestResult login = mvc.post().uri("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@festpage.com\",\"password\":\"admin123!\"}").exchange();
        assertThat(login).hasStatusOk();
        String bearer = "Bearer " + JsonPath.read(login.getResponse().getContentAsString(), "$.token");
        assertThat(mvc.get().uri("/api/admin/auth/me").header("Authorization", bearer)).hasStatusOk().bodyJson()
                .extractingPath("$.email").isEqualTo("admin@festpage.com");

        // Create a product with sizes.
        MvcTestResult created = mvc.post().uri("/api/admin/products").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Matalàs Test","slug":"matalas-test","price":300,"category":"MATTRESS","featured":true,
                         "sizes":[{"width":150,"length":190,"price":350},{"width":90,"length":190,"price":300}]}""")
                .exchange();
        assertThat(created).hasStatus(201).bodyJson().extractingPath("$.id").asString().matches("^c[0-9a-z]{24}$");
        String productId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        assertThat(mvc.get().uri("/api/products/matalas-test")).bodyJson().extractingPath("$.sizes[*].width")
                .asArray().containsExactly(90, 150);

        // Place an order: price frozen at order time.
        MvcTestResult order = mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerName":"Joan","customerEmail":"joan@example.com",
                 "items":[{"productId":"%s","quantity":2}]}""".formatted(productId)).exchange();
        assertThat(order).hasStatus(201).bodyJson().extractingPath("$.items[0].unitPrice").isEqualTo(300.0);
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.id");

        mvc.put().uri("/api/admin/products/" + productId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"price\":999}").exchange();
        assertThat(mvc.get().uri("/api/admin/orders").header("Authorization", bearer)).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("""
                        {"total":1,"data":[{"status":"PENDING","customer":{"email":"joan@example.com"},
                         "items":[{"unitPrice":300.00,"product":{"name":"Matalàs Test","price":999.00}}]}]}""");
        assertThat(mvc.patch().uri("/api/admin/orders/" + orderId + "/status").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}")).hasStatusOk();

        // A product with orders cannot be deleted.
        assertThat(mvc.delete().uri("/api/admin/products/" + productId).header("Authorization", bearer))
                .hasStatus(409);

        // Contact form (Turnstile not configured here) and the back-office inbox.
        assertThat(mvc.post().uri("/api/contacts").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Maria","email":"maria@example.com","message":"Voldria informació sobre matalassos."}"""))
                .hasStatus(201);
        MvcTestResult inbox = mvc.get().uri("/api/admin/contacts?read=false").header("Authorization", bearer).exchange();
        assertThat(inbox).bodyJson().extractingPath("$.total").isEqualTo(1);
        String contactId = JsonPath.read(inbox.getResponse().getContentAsString(), "$.data[0].id");
        assertThat(mvc.patch().uri("/api/admin/contacts/" + contactId + "/read").header("Authorization", bearer))
                .hasStatusOk();
        assertThat(mvc.delete().uri("/api/admin/contacts/" + contactId).header("Authorization", bearer))
                .hasStatus(204);

        assertThat(mvc.get().uri("/health")).hasStatusOk();
        assertThat(mvc.get().uri("/docs/json")).hasStatusOk();
    }

    @Test
    void theSeedScriptIsIdempotent() throws IOException {
        String seed = Files.readString(Path.of("../dev/seed.sql"));
        jdbc.sql(seed).update();
        jdbc.sql(seed).update();

        assertThat(jdbc.sql("select count(*) from \"AdminUser\"").query(Integer.class).single()).isEqualTo(1);
    }
}
