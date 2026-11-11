package com.festdescans.api.adapter.out.persistence;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * PostgreSQL adapter. The schema came from Prisma: quoted camelCase identifiers ("Product"."createdAt"),
 * so names are used exactly as written (no snake_case) and always quoted ("Order" is a reserved word).
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = PersistenceConfiguration.class)
@Import(FlywayMigrationConfiguration.class)
public class PersistenceConfiguration {

    @Bean
    HibernatePropertiesCustomizer prismaSchemaNaming() {
        return properties -> {
            properties.put("hibernate.physical_naming_strategy", new PhysicalNamingStrategyStandardImpl());
            properties.put("hibernate.globally_quoted_identifiers", "true");
            properties.put("hibernate.hbm2ddl.auto", "none");
            properties.put("hibernate.default_batch_fetch_size", "50");
            // No hibernate.jdbc.time_zone: entities hold UTC wall time as LocalDateTime (EntityMapper), and that
            // setting would shift it by the JVM's zone. PersistenceAdaptersTest checks the stored value.
        };
    }
}
