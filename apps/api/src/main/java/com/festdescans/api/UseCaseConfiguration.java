package com.festdescans.api;

import com.festdescans.api.adapter.out.password.Argon2PasswordHasher;
import com.festdescans.api.adapter.out.turnstile.TurnstileCaptchaVerifier;
import com.festdescans.api.application.port.out.AdminUserRepository;
import com.festdescans.api.application.port.out.CaptchaVerifier;
import com.festdescans.api.application.port.out.ContactRepository;
import com.festdescans.api.application.port.out.CustomerRepository;
import com.festdescans.api.application.port.out.IdGenerator;
import com.festdescans.api.application.port.out.OrderRepository;
import com.festdescans.api.application.port.out.PasswordVerifier;
import com.festdescans.api.application.port.out.ProductRepository;
import com.festdescans.api.application.port.out.TransactionRunner;
import com.festdescans.api.application.service.AdminAuthService;
import com.festdescans.api.application.service.ContactService;
import com.festdescans.api.application.service.OrderService;
import com.festdescans.api.application.service.ProductService;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The application layer has no Spring annotations: its services and the plain-Java adapters are wired here. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TurnstileProperties.class)
class UseCaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(UseCaseConfiguration.class);

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordVerifier passwordVerifier() {
        return new Argon2PasswordHasher();
    }

    @Bean
    CaptchaVerifier captchaVerifier(TurnstileProperties turnstile) {
        TurnstileCaptchaVerifier verifier =
                new TurnstileCaptchaVerifier(turnstile.secretKey(), turnstile.verifyUrl(), turnstile.timeout());
        if (!verifier.enabled()) {
            log.warn("TURNSTILE_SECRET_KEY is not set: the contact form captcha is NOT enforced");
        }
        return verifier;
    }

    @Bean
    ProductService productService(ProductRepository products, IdGenerator ids, TransactionRunner tx, Clock clock) {
        return new ProductService(products, ids, tx, clock);
    }

    @Bean
    ContactService contactService(ContactRepository contacts, CaptchaVerifier captcha, IdGenerator ids, Clock clock) {
        return new ContactService(contacts, captcha, ids, clock);
    }

    @Bean
    OrderService orderService(OrderRepository orders, CustomerRepository customers, ProductRepository products,
            IdGenerator ids, TransactionRunner tx, Clock clock) {
        return new OrderService(orders, customers, products, ids, tx, clock);
    }

    @Bean
    AdminAuthService adminAuthService(AdminUserRepository admins, PasswordVerifier passwords) {
        return new AdminAuthService(admins, passwords);
    }
}
