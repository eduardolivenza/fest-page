package com.festdescans.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FestPage REST api. Hexagonal layout, one Gradle module per layer:
 * domain <- application <- adapters (in: web; out: persistence, turnstile, password) <- bootstrap (this).
 * Component scanning from this package picks up each adapter's configuration; {@link UseCaseConfiguration}
 * wires the framework-free use cases to them.
 */
@SpringBootApplication
public class FestPageApplication {

    public static void main(String[] args) {
        SpringApplication.run(FestPageApplication.class, args);
    }
}
