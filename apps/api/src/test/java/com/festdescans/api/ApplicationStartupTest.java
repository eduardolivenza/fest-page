package com.festdescans.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.adapter.out.persistence.EmbeddedPostgresDatabase;
import com.festdescans.api.application.port.in.AuthenticateAdminUseCase;
import com.festdescans.api.application.port.in.BrowseProductsUseCase;
import com.festdescans.api.application.port.in.ManageContactsUseCase;
import com.festdescans.api.application.port.in.ManageOrdersUseCase;
import com.festdescans.api.application.port.in.ManageProductsUseCase;
import com.festdescans.api.application.port.in.PlaceOrderUseCase;
import com.festdescans.api.application.port.in.SubmitContactUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The application starts exactly as in production: through {@code FestPageApplication.main}, the full context
 * (every adapter, Flyway, Hibernate, security, Actuator) and a real Tomcat on a random port.
 */
@SpringBootTest(classes = FestPageApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = UseMainMethod.ALWAYS, properties = "festpage.web.jwt-secret=startup-test-secret-0123456789abcdef")
class ApplicationStartupTest {

    private static final EmbeddedPostgresDatabase DB = EmbeddedPostgresDatabase.create();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::jdbcUrl);
        registry.add("spring.datasource.username", DB::username);
        registry.add("spring.datasource.password", DB::password);
    }

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext context;

    @Test
    void wiresEveryUseCase() {
        for (Class<?> useCase : new Class<?>[] {BrowseProductsUseCase.class, ManageProductsUseCase.class,
                SubmitContactUseCase.class, ManageContactsUseCase.class, PlaceOrderUseCase.class,
                ManageOrdersUseCase.class, AuthenticateAdminUseCase.class}) {
            assertThat(context.getBeanProvider(useCase).getIfUnique()).as(useCase.getSimpleName()).isNotNull();
        }
    }

    @Test
    void servesHttpRequestsOnceStarted() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        for (String path : new String[] {"/health/readiness", "/health", "/api/products"}) {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(path).isEqualTo(200);
        }
    }

    @Test
    void refusesToStartWithoutAJwtSecret() {
        // Command-line arguments: they override application.yml, like env vars do in production.
        assertThatThrownBy(() -> SpringApplication.run(FestPageApplication.class, "--server.port=0",
                "--festpage.web.jwt-secret=", "--spring.datasource.url=" + DB.jdbcUrl(),
                "--spring.datasource.username=" + DB.username(), "--spring.datasource.password=" + DB.password())
                .close())
                .hasStackTraceContaining("JWT_SECRET must be set");
    }
}
