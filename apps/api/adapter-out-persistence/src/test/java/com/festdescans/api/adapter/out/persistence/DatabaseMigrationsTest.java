package com.festdescans.api.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class DatabaseMigrationsTest {

    private static final List<String> PRISMA_ORDER =
            List.of("20260524132142_test_1", "20260524133555_v2", "20260525081137_v3");

    @Test
    void runsOnTheSamePostgresMajorAsProduction() {
        assertThat(EmbeddedPostgresDatabase.create().serverVersion()).startsWith("18.");
    }

    @Test
    void createsTheSchemaInAnEmptyDatabase() {
        DataSource ds = EmbeddedPostgresDatabase.create().dataSource();

        MigrateResult result = DatabaseMigrations.migrate(ds);

        assertThat(result.migrationsExecuted).isEqualTo(3);
        JdbcClient jdbc = JdbcClient.create(ds);
        assertThat(columns(jdbc, "Product")).contains("createdAt", "featured").doesNotContain("stock");
        assertThat(columns(jdbc, "ProductSize")).contains("width", "length", "price", "productId");
        assertThat(DatabaseMigrations.migrate(ds).migrationsExecuted).as("idempotent").isZero();
    }

    @Test
    void baselinesADatabaseCreatedByPrismaWithoutTouchingItsData() {
        DataSource ds = EmbeddedPostgresDatabase.create().dataSource();
        createPrismaDatabase(ds, PRISMA_ORDER);
        JdbcClient jdbc = JdbcClient.create(ds);
        jdbc.sql("""
                insert into "Product" (id, name, slug, price, category, "updatedAt")
                values ('cprod1', 'Matalàs', 'matalas', 499.90, 'MATTRESS', now())""").update();

        MigrateResult result = DatabaseMigrations.migrate(ds);

        assertThat(result.migrationsExecuted).isZero();
        assertThat(jdbc.sql("select version, type from flyway_schema_history").query().listOfRows())
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.get("version")).isEqualTo("3");
                    assertThat(row.get("type")).isEqualTo("BASELINE");
                });
        assertThat(jdbc.sql("select name from \"Product\" where id = 'cprod1'").query(String.class).single())
                .isEqualTo("Matalàs");
        assertThat(DatabaseMigrations.migrate(ds).migrationsExecuted).isZero();
    }

    @Test
    void refusesToBaselineWhenAPrismaMigrationIsMissing() {
        DataSource ds = EmbeddedPostgresDatabase.create().dataSource();
        createPrismaDatabase(ds, PRISMA_ORDER.subList(0, 2));

        assertThatThrownBy(() -> DatabaseMigrations.migrate(ds))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("20260525081137_v3");
        assertThat(tableExists(ds, "flyway_schema_history")).isFalse();
    }

    @Test
    void refusesToBaselineWhenPrismaHasAnUnknownMigration() {
        DataSource ds = EmbeddedPostgresDatabase.create().dataSource();
        createPrismaDatabase(ds, PRISMA_ORDER);
        recordPrismaMigration(JdbcClient.create(ds), "20261201000000_newer");

        assertThatThrownBy(() -> DatabaseMigrations.migrate(ds)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesToBaselineASchemaWithoutAnyHistory() {
        DataSource ds = EmbeddedPostgresDatabase.create().dataSource();
        JdbcClient.create(ds).sql("create table something_else (id int)").update();

        assertThatThrownBy(() -> DatabaseMigrations.migrate(ds))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("neither a Flyway nor a Prisma history");
    }

    /** What production looks like: the Prisma SQL applied and recorded in _prisma_migrations. */
    private static void createPrismaDatabase(DataSource ds, List<String> recorded) {
        JdbcClient jdbc = JdbcClient.create(ds);
        for (String script : List.of("V1__init.sql", "V2__product_sizes.sql", "V3__drop_product_stock.sql")) {
            try (Connection c = ds.getConnection()) {
                c.createStatement().execute(resource("db/migration/" + script));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
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
        try (InputStream in = DatabaseMigrationsTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
