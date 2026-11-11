package com.festdescans.api;

import ch.qos.logback.classic.Level;
import com.festdescans.api.adapter.out.persistence.DatabaseMigrations;
import org.flywaydb.core.api.output.MigrateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the Flyway migrations and exits, without starting Spring. Run by the pod's migrate initContainer
 * (in a retry loop until PostgreSQL accepts connections). Exit code 0 on success, 1 on failure.
 *
 * <p>Reads SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD, like the app.
 */
public final class MigrateDatabase {

    private MigrateDatabase() {
    }

    public static void main(String[] args) {
        // Without Spring Boot's logging setup Logback would log everything at DEBUG.
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        try {
            MigrateResult result = DatabaseMigrations.migrate(
                    env("SPRING_DATASOURCE_URL"), env("SPRING_DATASOURCE_USERNAME"), env("SPRING_DATASOURCE_PASSWORD"));
            System.out.printf("Database migrated: %d migration(s) applied, schema version %s%n",
                    result.migrationsExecuted, result.targetSchemaVersion == null ? "unchanged" : result.targetSchemaVersion);
        } catch (RuntimeException e) {
            System.err.println("Migration failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not set");
        }
        return value;
    }
}
