package com.festdescans.api.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Boot's Flyway auto-configuration with our defaults and Prisma guard, on a real PostgreSQL. */
class FlywayMigrationTest {

    private static final List<String> PRISMA_ORDER =
            List.of("20260524132142_test_1", "20260524133555_v2", "20260525081137_v3");

    private static ApplicationContextRunner migrating(EmbeddedPostgresDatabase db) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class))
                .withUserConfiguration(FlywayMigrationConfiguration.class)
                .withPropertyValues("spring.datasource.url=" + db.jdbcUrl(),
                        "spring.datasource.username=" + db.username(), "spring.datasource.password=" + db.password());
    }

    private static List<Map<String, Object>> history(DataSource ds) {
        return JdbcClient.create(ds).sql("select version, type from flyway_schema_history order by installed_rank")
                .query().listOfRows();
    }

    @Test
    void runsOnTheSamePostgresMajorAsProduction() {
        assertThat(EmbeddedPostgresDatabase.create().serverVersion()).startsWith("18.");
    }

    @Test
    void createsTheSchemaInAnEmptyDatabase() {
        EmbeddedPostgresDatabase db = EmbeddedPostgresDatabase.create();

        migrating(db).run(ctx -> assertThat(ctx).hasNotFailed());
        migrating(db).run(ctx -> assertThat(ctx).as("idempotent").hasNotFailed());

        DataSource ds = db.dataSource();
        assertThat(history(ds)).extracting(r -> r.get("version")).containsExactly("1", "2", "3");
        JdbcClient jdbc = JdbcClient.create(ds);
        assertThat(columns(jdbc, "Product")).contains("createdAt", "featured").doesNotContain("stock");
        assertThat(columns(jdbc, "ProductSize")).contains("width", "length", "price", "productId");
    }

    @Test
    void baselinesADatabaseCreatedByPrismaWithoutTouchingItsData() {
        EmbeddedPostgresDatabase db = EmbeddedPostgresDatabase.create();
        DataSource ds = db.dataSource();
        createPrismaDatabase(ds, PRISMA_ORDER);
        JdbcClient.create(ds).sql("""
                insert into "Product" (id, name, slug, price, category, "updatedAt")
                values ('cprod1', 'Matalàs', 'matalas', 499.90, 'MATTRESS', now())""").update();

        migrating(db).run(ctx -> assertThat(ctx).hasNotFailed());
        migrating(db).run(ctx -> assertThat(ctx).hasNotFailed());

        assertThat(history(ds)).singleElement().satisfies(row -> {
            assertThat(row.get("version")).isEqualTo("3");
            assertThat(row.get("type")).isEqualTo("BASELINE");
        });
        assertThat(JdbcClient.create(ds).sql("select name from \"Product\" where id = 'cprod1'").query(String.class)
                .single()).isEqualTo("Matalàs");
    }

    @Test
    void refusesToBaselineWhenAPrismaMigrationIsMissing() {
        EmbeddedPostgresDatabase db = EmbeddedPostgresDatabase.create();
        createPrismaDatabase(db.dataSource(), PRISMA_ORDER.subList(0, 2));

        migrating(db).run(ctx -> assertThat(ctx).hasFailed().getFailure()
                .rootCause().hasMessageContaining("20260525081137_v3"));
        assertThat(tableExists(db.dataSource(), "flyway_schema_history")).isFalse();
    }

    @Test
    void refusesToBaselineWhenPrismaHasAnUnknownMigration() {
        EmbeddedPostgresDatabase db = EmbeddedPostgresDatabase.create();
        createPrismaDatabase(db.dataSource(), PRISMA_ORDER);
        recordPrismaMigration(JdbcClient.create(db.dataSource()), "20261201000000_newer");

        migrating(db).run(ctx -> assertThat(ctx).hasFailed());
        assertThat(tableExists(db.dataSource(), "flyway_schema_history")).isFalse();
    }

    @Test
    void refusesToBaselineASchemaWithoutAnyHistory() {
        EmbeddedPostgresDatabase db = EmbeddedPostgresDatabase.create();
        JdbcClient.create(db.dataSource()).sql("create table something_else (id int)").update();

        migrating(db).run(ctx -> assertThat(ctx).hasFailed().getFailure()
                .rootCause().hasMessageContaining("neither a Flyway nor a Prisma history"));
    }

    /** What production looks like: the Prisma SQL applied and recorded in _prisma_migrations. */
    private static void createPrismaDatabase(DataSource ds, List<String> recorded) {
        for (String script : List.of("V1__init.sql", "V2__product_sizes.sql", "V3__drop_product_stock.sql")) {
            try (Connection c = ds.getConnection()) {
                c.createStatement().execute(resource("db/migration/" + script));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
        JdbcClient jdbc = JdbcClient.create(ds);
        jdbc.sql("""
                CREATE TABLE "_prisma_migrations" (
                    "id" VARCHAR(36) PRIMARY KEY NOT NULL,
                    "checksum" VARCHAR(64) NOT NULL,
                    "finished_at" TIMESTAMPTZ,
                    "migration_name" VARCHAR(255) NOT NULL,
                    "logs" TEXT,
                    "rolled_back_at" TIMESTAMPTZ,
                    "started_at" TIMESTAMPTZ NOT NULL DEFAULT now(),
                    "applied_steps_count" INTEGER NOT NULL DEFAULT 0
                )""").update();
        recorded.forEach(name -> recordPrismaMigration(jdbc, name));
    }

    private static void recordPrismaMigration(JdbcClient jdbc, String name) {
        jdbc.sql("""
                insert into "_prisma_migrations" (id, checksum, finished_at, migration_name, applied_steps_count)
                values (gen_random_uuid()::text, 'x', now(), ?, 1)""").param(name).update();
    }

    private static List<String> columns(JdbcClient jdbc, String table) {
        return jdbc.sql("select column_name from information_schema.columns where table_name = ?")
                .param(table).query(String.class).list();
    }

    private static boolean tableExists(DataSource ds, String table) {
        return !JdbcClient.create(ds).sql("select 1 from information_schema.tables where table_name = ?")
                .param(table).query().listOfRows().isEmpty();
    }

    private static String resource(String path) {
        try (InputStream in = FlywayMigrationTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
