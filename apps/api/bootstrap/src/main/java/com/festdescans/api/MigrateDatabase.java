package com.festdescans.api;

import com.festdescans.api.adapter.out.persistence.FlywayMigrationConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Applies the Flyway migrations and exits: the pod's migrate initContainer runs it in a retry loop until
 * PostgreSQL accepts connections. Same configuration as the app (application.yml, SPRING_DATASOURCE_* env,
 * Boot's Flyway auto-configuration, Prisma guard) but only DataSource + Flyway: no web server, JPA or
 * security, so it needs no JWT_SECRET and starts in a couple of seconds. Exit code 1 on failure.
 */
public final class MigrateDatabase {

    private MigrateDatabase() {
    }

    // Deliberately not a @Configuration: component scanning and @SpringBootTest must not pick it up.
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
    @Import(FlywayMigrationConfiguration.class)
    static final class Migration {
    }

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Migration.class)
                .web(WebApplicationType.NONE)
                .run(args)) {
            MigrationInfo current = context.getBean(Flyway.class).info().current();
            System.out.println("Database schema at version " + (current == null ? "none" : current.getVersion()));
        }
    }
}
