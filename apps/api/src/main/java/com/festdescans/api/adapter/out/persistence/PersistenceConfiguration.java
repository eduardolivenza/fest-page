package com.festdescans.api.adapter.out.persistence;

import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Configuration;

/**
 * PostgreSQL adapter. The schema came from Prisma: quoted camelCase identifiers ("Product"."createdAt"),
 * so names are used exactly as written (no snake_case) and always quoted ("Order" is a reserved word).
 */
@Configuration(proxyBeanMethods = false)
// Same exclude filter as @SpringBootApplication: test-only configurations are never scanned.
@ComponentScan(basePackageClasses = PersistenceConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class))
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
