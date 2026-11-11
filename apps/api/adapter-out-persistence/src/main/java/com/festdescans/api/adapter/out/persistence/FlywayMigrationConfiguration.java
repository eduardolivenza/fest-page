package com.festdescans.api.adapter.out.persistence;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Flyway owns the schema (db/migration; V1-V3 are the former Prisma migrations). Boot's Flyway
 * auto-configuration runs them on startup, before Hibernate; this adds the Prisma baseline settings
 * (flyway-defaults.properties) and the check that must pass before baselining.
 * Used by the app and by the initContainer's MigrateDatabase.
 */
@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:db/flyway-defaults.properties")
public class FlywayMigrationConfiguration {

    @Bean
    FlywayMigrationStrategy checkPrismaHistoryThenMigrate() {
        return flyway -> {
            PrismaBaselineGuard.check(flyway.getConfiguration().getDataSource());
            flyway.migrate();
        };
    }
}
