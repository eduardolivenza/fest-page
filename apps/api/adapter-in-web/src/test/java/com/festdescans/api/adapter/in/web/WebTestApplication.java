package com.festdescans.api.adapter.in.web;

import com.festdescans.api.adapter.out.password.Argon2PasswordHasher;
import com.festdescans.api.application.fakes.DirectTransactionRunner;
import com.festdescans.api.application.fakes.InMemoryDatabase;
import com.festdescans.api.application.fakes.RecordingCaptchaVerifier;
import com.festdescans.api.application.fakes.SequentialIdGenerator;
import com.festdescans.api.application.service.AdminAuthService;
import com.festdescans.api.application.service.ContactService;
import com.festdescans.api.application.service.OrderService;
import com.festdescans.api.application.service.ProductService;
import java.time.Clock;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * The web adapter driving the real use case services, with in-memory outbound adapters
 * (the equivalent of the Node api tests' fake Prisma).
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(WebAdapterConfiguration.class)
class WebTestApplication {

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
