package com.festdescans.api.adapter.in.web;

import static com.festdescans.api.application.fakes.Fixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;

import com.festdescans.api.application.fakes.Fixtures;
import com.festdescans.api.application.fakes.RecordingCaptchaVerifier.Check;
import com.festdescans.api.domain.contact.ContactRequest;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.product.ProductCategory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Migrated from the Node api's test/public.test.ts. */
class PublicApiTest extends WebTestBase {

    private MvcTestResult postJson(String uri, String json, String... headers) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
        for (int i = 0; i < headers.length; i += 2) {
            request = request.header(headers[i], headers[i + 1]);
        }
        return request.exchange();
    }

    @Nested
    class Health {

        @Test
        void returnsOk() {
            assertThat(mvc.get().uri("/health")).hasStatusOk().bodyJson().isStrictlyEqualTo("{\"status\":\"ok\"}");
        }
    }

    @Nested
    class Products {

        @Test
        void returnsAPaginatedListWithNumericPrices() {
            db.products.put("p1", Fixtures.withImagesAndSizes(Fixtures.product("p1", "matalas", "499.90")));

            MvcTestResult res = mvc.get().uri("/api/products?pageSize=12").exchange();

            assertThat(res).hasStatusOk().bodyJson()
                    .isLenientlyEqualTo("{\"total\":1,\"page\":1,\"pageSize\":12,\"totalPages\":1}");
            assertThat(res).bodyJson().extractingPath("$.data[0].price").isEqualTo(499.9);
            assertThat(res).bodyJson().extractingPath("$.data[0].sizes[0].price").isEqualTo(499.9);
            assertThat(res).bodyJson().extractingPath("$.data[0].sizes[0].width").isEqualTo(90);
            // Only the first image (lowest position) in the listing.
            assertThat(res).bodyJson().extractingPath("$.data[0].images[*].id").asArray().containsExactly("img-1");
            assertThat(res).bodyJson().extractingPath("$.data[0].createdAt").isEqualTo("2026-11-01T10:00:00Z");
        }

        @Test
        void capsPageSizeAt50AndFiltersByCategory() {
            db.products.put("bed", Fixtures.product("bed", ProductCategory.BED, false, T0));
            db.products.put("pillow", Fixtures.product("pillow", ProductCategory.PILLOW, false, T0));

            MvcTestResult res = mvc.get().uri("/api/products?pageSize=500&category=PILLOW").exchange();

            assertThat(res).hasStatusOk().bodyJson().extractingPath("$.pageSize").isEqualTo(50);
            assertThat(res).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly("pillow");
        }

        @Test
        void filtersFeaturedProducts() {
            db.products.put("a", Fixtures.product("a", ProductCategory.BED, true, T0));
            db.products.put("b", Fixtures.product("b", ProductCategory.BED, false, T0));

            assertThat(mvc.get().uri("/api/products?featured=true")).bodyJson()
                    .extractingPath("$.data[*].id").asArray().containsExactly("a");
        }

        @Test
        void rejectsAnUnknownCategory() {
            assertThat(mvc.get().uri("/api/products?category=SOFA")).hasStatus(400);
        }

        @Test
        void returnsAProductByIdOrSlug() {
            db.products.put("p1", Fixtures.withImagesAndSizes(Fixtures.product("p1", "matalas", "499.90")));

            assertThat(mvc.get().uri("/api/products/matalas")).hasStatusOk().bodyJson()
                    .extractingPath("$.images[*].id").asArray().containsExactly("img-1", "img-2");
            assertThat(mvc.get().uri("/api/products/p1")).hasStatusOk();
        }

        @Test
        void returns404ForAnUnknownProduct() {
            assertThat(mvc.get().uri("/api/products/does-not-exist")).hasStatus(404).bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"Product not found\"}");
        }
    }

    @Nested
    class Contacts {

        private static final String VALID =
                "{\"name\":\"Maria\",\"email\":\"maria@example.com\",\"message\":\"Voldria informació sobre matalassos.\"}";

        @Test
        void rejectsInvalidDataWith400() {
            MvcTestResult res = postJson("/api/contacts", "{\"name\":\"M\",\"email\":\"nope\",\"message\":\"short\"}");

            assertThat(res).hasStatus(400).bodyJson().extractingPath("$.message").isEqualTo("Invalid data");
            assertThat(res).bodyJson().extractingPath("$.errors.fieldErrors").asMap()
                    .containsOnlyKeys("name", "email", "message");
            assertThat(db.contacts).isEmpty();
            assertThat(captcha.checks()).isEmpty();
        }

        @Test
        void savesAValidMessage() {
            MvcTestResult res = postJson("/api/contacts", VALID);

            assertThat(res).hasStatus(201).bodyJson()
                    .isLenientlyEqualTo("{\"name\":\"Maria\",\"email\":\"maria@example.com\",\"read\":false,\"phone\":null}");
            assertThat(db.contacts.values()).singleElement().extracting(ContactRequest::message)
                    .isEqualTo("Voldria informació sobre matalassos.");
        }

        @Test
        void passesTheTokenAndVisitorIpToTheCaptchaCheckWithoutStoringTheToken() {
            MvcTestResult res = postJson("/api/contacts",
                    VALID.replace("}", ",\"turnstileToken\":\"tok\"}"), "CF-Connecting-IP", "203.0.113.7");

            assertThat(res).hasStatus(201);
            assertThat(captcha.checks()).containsExactly(new Check("tok", "203.0.113.7"));
            assertThat(res).bodyJson().doesNotHavePath("$.turnstileToken");
        }

        @Test
        void rejectsWith403WhenTheCaptchaFails() {
            captcha.answer(false);

            assertThat(postJson("/api/contacts", VALID)).hasStatus(403).bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"Captcha verification failed\"}");
            assertThat(db.contacts).isEmpty();
        }

        @Test
        void rejectsMalformedJson() {
            assertThat(postJson("/api/contacts", "{not json")).hasStatus(400);
        }

        @Test
        void ignoresAStaleAdminTokenOnPublicRoutes() {
            assertThat(mvc.post().uri("/api/contacts").contentType(MediaType.APPLICATION_JSON).content(VALID)
                    .header("Authorization", "Bearer garbage")).hasStatus(201);
        }
    }

    @Nested
    class Orders {

        private static final String ORDER = """
                {"customerName":"Joan","customerEmail":"joan@example.com","items":[{"productId":"p1","quantity":2}]}""";

        @Test
        void rejectsAnOrderWithNoItems() {
            assertThat(postJson("/api/orders", ORDER.replace("[{\"productId\":\"p1\",\"quantity\":2}]", "[]")))
                    .hasStatus(400);
        }

        @Test
        void rejectsAQuantityThatIsNotAPositiveInteger() {
            assertThat(postJson("/api/orders", ORDER.replace("\"quantity\":2", "\"quantity\":0"))).hasStatus(400);
            assertThat(postJson("/api/orders", ORDER.replace("\"quantity\":2", "\"quantity\":2.5"))).hasStatus(400);
            assertThat(postJson("/api/orders", ORDER.replace("\"quantity\":2", "\"quantity\":\"2\""))).hasStatus(400);
        }

        @Test
        void rejectsAnOrderForUnknownProducts() {
            assertThat(postJson("/api/orders", ORDER)).hasStatus(400).bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"One or more products not found\"}");
            assertThat(db.orders).isEmpty();
            assertThat(db.customers).isEmpty();
        }

        @Test
        void createsAnOrderUsingTheProductPrice() {
            db.products.put("p1", Fixtures.product("p1", "matalas", "120.00"));

            MvcTestResult res = postJson("/api/orders", ORDER);

            assertThat(res).hasStatus(201).bodyJson().isLenientlyEqualTo("""
                    {"status":"PENDING","items":[{"productId":"p1","quantity":2,"unitPrice":120.00}]}""");
            Order order = db.orders.values().iterator().next();
            assertThat(res).bodyJson().extractingPath("$.customerId").isEqualTo(order.customer().id());
            assertThat(order.items().getFirst().unitPrice()).isEqualByComparingTo("120.00");
            assertThat(db.customers.values()).singleElement().satisfies(c -> assertThat(c.email()).isEqualTo("joan@example.com"));
        }
    }

    @Test
    void unknownRoutesAre404Json() {
        assertThat(mvc.get().uri("/api/nope")).hasStatus(404).bodyJson()
                .isStrictlyEqualTo("{\"message\":\"Not found\"}");
    }

    @Test
    void servesTheOpenApiDocument() {
        assertThat(mvc.get().uri("/docs/json")).hasStatusOk().bodyJson().extractingPath("$.info.title")
                .isEqualTo("FestPage API");
    }
}
