package com.festdescans.api.adapter.in.web;

import static com.festdescans.api.application.fakes.Fixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;

import com.festdescans.api.adapter.out.password.Argon2PasswordHasher;
import com.festdescans.api.application.fakes.Fixtures;
import com.festdescans.api.domain.admin.AdminUser;
import com.festdescans.api.domain.order.Order;
import com.festdescans.api.domain.order.OrderItem;
import com.festdescans.api.domain.order.OrderStatus;
import com.festdescans.api.domain.order.ProductSummary;
import com.festdescans.api.domain.product.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Migrated from the Node api's test/admin.test.ts, plus the admin endpoints it did not cover. */
class AdminApiTest extends WebTestBase {

    @Autowired
    Argon2PasswordHasher passwords;
    @Autowired
    JwtDecoder jwtDecoder;

    @Nested
    class Login {

        @Test
        void returnsATokenForValidCredentials() throws Exception {
            db.admins.put("admin-1", new AdminUser("admin-1", "admin@festdescans.com",
                    passwords.hash("correct-password"), "Admin", T0, T0));

            MvcTestResult res = mvc.post().uri("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"admin@festdescans.com\",\"password\":\"correct-password\"}").exchange();

            assertThat(res).hasStatusOk().bodyJson().extractingPath("$.user")
                    .isEqualTo(java.util.Map.of("id", "admin-1", "email", "admin@festdescans.com", "name", "Admin"));
            String token = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.token");
            var jwt = jwtDecoder.decode(token);
            assertThat(jwt.getSubject()).isEqualTo("admin-1");
            assertThat(jwt.getClaimAsString("email")).isEqualTo("admin@festdescans.com");
            assertThat(jwt.getExpiresAt()).isBetween(jwt.getIssuedAt().plusSeconds(8 * 3600 - 1),
                    jwt.getIssuedAt().plusSeconds(8 * 3600 + 1));
        }

        @Test
        void rejectsAWrongPassword() {
            db.admins.put("admin-1", new AdminUser("admin-1", "a@b.com", passwords.hash("right"), "Admin", T0, T0));

            assertThat(mvc.post().uri("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"a@b.com\",\"password\":\"wrong\"}")).hasStatus(401).bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"Invalid credentials\"}");
        }

        @Test
        void rejectsAnUnknownUser() {
            assertThat(mvc.post().uri("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"x@y.com\",\"password\":\"whatever\"}")).hasStatus(401);
        }

        @Test
        void rejectsInvalidInputWithTheSameMessage() {
            assertThat(mvc.post().uri("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"not-an-email\",\"password\":\"\"}")).hasStatus(400).bodyJson()
                    .isStrictlyEqualTo("{\"message\":\"Invalid credentials\"}");
        }

        @Test
        void meReturnsTheLoggedInAdmin() {
            db.admins.put("admin-1", new AdminUser("admin-1", "admin@festdescans.com", "x", "Admin", T0, T0));

            assertThat(mvc.get().uri("/api/admin/auth/me").header("Authorization", bearer())).hasStatusOk().bodyJson()
                    .isStrictlyEqualTo("""
                            {"id":"admin-1","email":"admin@festdescans.com","name":"Admin","createdAt":"2026-11-01T10:00:00Z"}""");
        }

        @Test
        void meIs401WhenTheAdminNoLongerExists() {
            assertThat(mvc.get().uri("/api/admin/auth/me").header("Authorization", bearer())).hasStatus(401);
        }
    }

    @Nested
    class Contacts {

        @Test
        void listsContacts() {
            db.contacts.put("c1", Fixtures.contact("c1", false, T0));

            assertThat(mvc.get().uri("/api/admin/contacts").header("Authorization", bearer())).hasStatusOk()
                    .bodyJson().isLenientlyEqualTo("{\"data\":[{\"id\":\"c1\"}],\"total\":1}");
        }

        @Test
        void filtersByReadState() {
            db.contacts.put("c1", Fixtures.contact("c1", false, T0));
            db.contacts.put("c2", Fixtures.contact("c2", true, T0));

            assertThat(mvc.get().uri("/api/admin/contacts?read=true").header("Authorization", bearer())).bodyJson()
                    .extractingPath("$.data[*].id").asArray().containsExactly("c2");
            assertThat(mvc.get().uri("/api/admin/contacts?read=false").header("Authorization", bearer())).bodyJson()
                    .extractingPath("$.data[*].id").asArray().containsExactly("c1");
        }

        @Test
        void marksAContactAsRead() {
            db.contacts.put("c1", Fixtures.contact("c1", false, T0));

            assertThat(mvc.patch().uri("/api/admin/contacts/c1/read").header("Authorization", bearer()))
                    .hasStatusOk().bodyJson().extractingPath("$.read").isEqualTo(true);
            assertThat(db.contacts.get("c1").read()).isTrue();
        }

        @Test
        void marking404sForAMissingContact() {
            assertThat(mvc.patch().uri("/api/admin/contacts/missing/read").header("Authorization", bearer()))
                    .hasStatus(404);
        }

        @Test
        void deletesAContact() {
            db.contacts.put("c1", Fixtures.contact("c1", false, T0));

            assertThat(mvc.delete().uri("/api/admin/contacts/c1").header("Authorization", bearer())).hasStatus(204);
            assertThat(db.contacts).isEmpty();
        }

        @Test
        void returns404WhenDeletingAContactThatDoesNotExist() {
            assertThat(mvc.delete().uri("/api/admin/contacts/missing").header("Authorization", bearer()))
                    .hasStatus(404).bodyJson().isStrictlyEqualTo("{\"message\":\"Not found\"}");
        }
    }

    @Nested
    class Products {

        private static final String NEW_PRODUCT = """
                {"name":"Coixí","slug":"coixi","price":49.99,"category":"PILLOW",
                 "sizes":[{"width":70,"length":40,"price":49.99}]}""";

        private MvcTestResult post(String json) {
            return mvc.post().uri("/api/admin/products").header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
        }

        @Test
        void createsAProduct() {
            MvcTestResult res = post(NEW_PRODUCT);

            assertThat(res).hasStatus(201).bodyJson().isLenientlyEqualTo("""
                    {"name":"Coixí","slug":"coixi","price":49.99,"category":"PILLOW","featured":false,
                     "sizes":[{"width":70,"length":40,"price":49.99}],"images":[]}""");
            assertThat(db.products.values()).singleElement().extracting(Product::slug).isEqualTo("coixi");
        }

        @Test
        void rejectsInvalidProducts() {
            assertThat(post("{\"name\":\"Coixí\"}")).hasStatus(400).bodyJson().extractingPath("$.errors.fieldErrors")
                    .asMap().containsOnlyKeys("slug", "price", "category");
            assertThat(post(NEW_PRODUCT.replace("coixi", "Coixí Bo"))).hasStatus(400);
            assertThat(post(NEW_PRODUCT.replace("\"PILLOW\"", "\"SOFA\""))).hasStatus(400);
            assertThat(post(NEW_PRODUCT.replace("\"price\":49.99,\"category\"", "\"price\":-1,\"category\"")))
                    .hasStatus(400);
            assertThat(db.products).isEmpty();
        }

        @Test
        void rejectsADuplicateSlugWith409() {
            db.products.put("p1", Fixtures.product("p1", "coixi", "10"));

            assertThat(post(NEW_PRODUCT)).hasStatus(409);
        }

        @Test
        void getsAndListsProducts() {
            db.products.put("p1", Fixtures.withImagesAndSizes(Fixtures.product("p1", "a", "10")));

            assertThat(mvc.get().uri("/api/admin/products/p1").header("Authorization", bearer())).hasStatusOk()
                    .bodyJson().extractingPath("$.images[*].id").asArray().containsExactly("img-1", "img-2");
            assertThat(mvc.get().uri("/api/admin/products/nope").header("Authorization", bearer())).hasStatus(404);
            assertThat(mvc.get().uri("/api/admin/products?pageSize=1000").header("Authorization", bearer()))
                    .bodyJson().isLenientlyEqualTo("{\"total\":1,\"pageSize\":100}");
        }

        @Test
        void updatesOnlyTheGivenFieldsAndReplacesSizes() {
            db.products.put("p1", Fixtures.withImagesAndSizes(Fixtures.product("p1", "a", "10")));

            MvcTestResult res = mvc.put().uri("/api/admin/products/p1").header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"price\":12.5,\"sizes\":[{\"width\":160,\"length\":200,\"price\":700}]}").exchange();

            assertThat(res).hasStatusOk().bodyJson().isLenientlyEqualTo(
                    "{\"slug\":\"a\",\"price\":12.50,\"sizes\":[{\"width\":160,\"length\":200,\"price\":700.00}]}");
            assertThat(db.products.get("p1").images()).hasSize(2);
            assertThat(mvc.put().uri("/api/admin/products/nope").header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).hasStatus(404);
        }

        @Test
        void deletesAProductUnlessItHasOrders() {
            db.products.put("p1", Fixtures.product("p1", "a", "10"));
            db.products.put("p2", Fixtures.product("p2", "b", "10"));
            db.customers.put("cu1", Fixtures.customer("cu1", "joan@example.com"));
            db.orders.put("o1", new Order("o1", db.customers.get("cu1"),
                    List.of(new OrderItem("i1", "o1", new ProductSummary("p2", "b", BigDecimal.TEN), 1, BigDecimal.TEN)),
                    OrderStatus.PENDING, null, T0, T0));

            assertThat(mvc.delete().uri("/api/admin/products/p1").header("Authorization", bearer())).hasStatus(204);
            assertThat(mvc.delete().uri("/api/admin/products/p1").header("Authorization", bearer())).hasStatus(404);
            assertThat(mvc.delete().uri("/api/admin/products/p2").header("Authorization", bearer())).hasStatus(409)
                    .bodyJson().isStrictlyEqualTo(
                            "{\"message\":\"No se puede eliminar: el producto tiene pedidos asociados\"}");
        }
    }

    @Nested
    class Orders {

        private void seedOrder() {
            db.products.put("p1", Fixtures.product("p1", "a", "120.00"));
            db.customers.put("cu1", Fixtures.customer("cu1", "joan@example.com"));
            db.orders.put("o1", new Order("o1", db.customers.get("cu1"),
                    List.of(new OrderItem("i1", "o1", new ProductSummary("p1", "Product p1", new BigDecimal("120.00")),
                            2, new BigDecimal("110.00"))),
                    OrderStatus.PENDING, "Matí", T0, T0));
        }

        @Test
        void listsOrdersWithCustomerAndProducts() {
            seedOrder();

            MvcTestResult res = mvc.get().uri("/api/admin/orders").header("Authorization", bearer()).exchange();

            assertThat(res).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                    {"total":1,"data":[{"id":"o1","customerId":"cu1","status":"PENDING","notes":"Matí",
                      "customer":{"id":"cu1","email":"joan@example.com"},
                      "items":[{"id":"i1","orderId":"o1","productId":"p1","quantity":2,"unitPrice":110.00,
                                "product":{"id":"p1","name":"Product p1","price":120.00}}]}]}""");
            assertThat(mvc.get().uri("/api/admin/orders?status=CONFIRMED").header("Authorization", bearer()))
                    .bodyJson().extractingPath("$.total").isEqualTo(0);
        }

        @Test
        void changesTheStatus() {
            seedOrder();

            assertThat(mvc.patch().uri("/api/admin/orders/o1/status").header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                    .hasStatusOk().bodyJson().isLenientlyEqualTo("{\"id\":\"o1\",\"status\":\"CONFIRMED\"}");
            assertThat(db.orders.get("o1").status()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        void rejectsAnInvalidStatusAndAMissingOrder() {
            seedOrder();

            assertThat(mvc.patch().uri("/api/admin/orders/o1/status").header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                    .hasStatus(400).bodyJson().isStrictlyEqualTo("{\"message\":\"Invalid status\"}");
            assertThat(mvc.patch().uri("/api/admin/orders/nope/status").header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}")).hasStatus(404);
        }
    }
}
