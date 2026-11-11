package com.festdescans.api.adapter.in.web;

import com.festdescans.api.adapter.in.web.security.AdminTokens;
import com.festdescans.api.adapter.out.password.Argon2PasswordHasher;
import com.festdescans.api.application.fakes.DirectTransactionRunner;
import com.festdescans.api.application.fakes.Fixtures;
import com.festdescans.api.application.fakes.InMemoryDatabase;
import com.festdescans.api.application.fakes.RecordingCaptchaVerifier;
import com.festdescans.api.application.fakes.SequentialIdGenerator;
import com.festdescans.api.application.service.AdminAuthService;
import com.festdescans.api.application.service.ContactService;
import com.festdescans.api.application.service.OrderService;
import com.festdescans.api.application.service.ProductService;
import com.festdescans.api.domain.admin.AdminUser;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest(classes = WebTestBase.App.class, properties = {
        // Same secret the Node-signed fixture tokens in SecurityTest were created with.
        "festpage.web.jwt-secret=" + WebTestBase.JWT_SECRET,
        "festpage.web.cors-origins=https://festdescans.com,https://admin.festdescans.com",
})
@AutoConfigureMockMvc
abstract class WebTestBase {

    static final String JWT_SECRET = "compat-test-secret-0123456789abcdef";

    @Autowired
    protected MockMvcTester mvc;
    @Autowired
    protected InMemoryDatabase db;
    @Autowired
    protected RecordingCaptchaVerifier captcha;
    @Autowired
    protected SequentialIdGenerator ids;
    @Autowired
    protected AdminTokens tokens;

    @BeforeEach
    void resetState() {
        db.clear();
        captcha.reset();
        ids.reset();
    }

    protected String adminToken() {
        return tokens.issue(new AdminUser("admin-1", "admin@festdescans.com", "", "Admin", Fixtures.T0, Fixtures.T0));
    }

    protected String bearer() {
        return "Bearer " + adminToken();
    }

    /**
     * The web adapter driving the real use case services, with in-memory outbound adapters
     * (the equivalent of the Node api tests' fake Prisma): no database. Deliberately not a @Configuration,
     * so the application's component scan never picks it up.
     */
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class})
    @Import(WebAdapterConfiguration.class)
    static class App {

        @Bean
        InMemoryDatabase database() {
            return new InMemoryDatabase();
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        SequentialIdGenerator ids() {
            return new SequentialIdGenerator();
        }

        @Bean
        DirectTransactionRunner transactions() {
            return new DirectTransactionRunner();
        }

        @Bean
        RecordingCaptchaVerifier captcha() {
            return new RecordingCaptchaVerifier();
        }

        @Bean
        Argon2PasswordHasher passwords() {
            return new Argon2PasswordHasher();
        }

        @Bean
        ProductService productService(InMemoryDatabase db, SequentialIdGenerator ids, DirectTransactionRunner tx,
                Clock clock) {
            return new ProductService(db.productRepository, ids, tx, clock);
        }

        @Bean
        ContactService contactService(InMemoryDatabase db, RecordingCaptchaVerifier captcha, SequentialIdGenerator ids,
                Clock clock) {
            return new ContactService(db.contactRepository, captcha, ids, clock);
        }

        @Bean
        OrderService orderService(InMemoryDatabase db, SequentialIdGenerator ids, DirectTransactionRunner tx,
                Clock clock) {
            return new OrderService(db.orderRepository, db.customerRepository, db.productRepository, ids, tx, clock);
        }

        @Bean
        AdminAuthService adminAuthService(InMemoryDatabase db, Argon2PasswordHasher passwords) {
            return new AdminAuthService(db.adminUserRepository, passwords);
        }
}
}
