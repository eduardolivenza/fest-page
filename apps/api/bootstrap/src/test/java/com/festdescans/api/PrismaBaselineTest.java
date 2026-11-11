package com.festdescans.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.festdescans.api.adapter.out.persistence.EmbeddedPostgresDatabase;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The first start on production: a database created by Prisma (schema + data, no Flyway history).
 * The app must record a baseline at V3, change nothing, and serve the existing data.
 */
@SpringBootTest(properties = "festpage.web.jwt-secret=end-to-end-test-secret-0123456789abcdef")
@AutoConfigureMockMvc
class PrismaBaselineTest {

    private static final EmbeddedPostgresDatabase DB = prismaDatabase();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DB::jdbcUrl);
        registry.add("spring.datasource.username", DB::username);
        registry.add("spring.datasource.password", DB::password);
    }

    @Autowired
    JdbcClient jdbc;
    @Autowired
    MockMvcTester mvc;

    @Test
    void baselinesTheSchemaAndServesTheExistingData() {
        assertThat(jdbc.sql("select version || ' ' || type from flyway_schema_history").query(String.class).list())
                .containsExactly("3 BASELINE");
        assertThat(mvc.get().uri("/api/products/matalas")).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("{\"id\":\"cprod1\",\"name\":\"Matalàs\",\"price\":499.90}");
    }

    /** What production looks like before the first Java deploy: the three Prisma migrations, applied by Prisma. */
    private static EmbeddedPostgresDatabase prismaDatabase() {
        EmbeddedPostgresDatabase db = EmbeddedPostgresDatabase.create();
        DataSource ds = db.dataSource();
        for (String script : new String[] {"V1__init.sql", "V2__product_sizes.sql", "V3__drop_product_stock.sql"}) {
            execute(ds, resource("db/migration/" + script));
        }
        execute(ds, """
                CREATE TABLE "_prisma_migrations" ("id" VARCHAR(36) PRIMARY KEY, "migration_name" VARCHAR(255) NOT NULL);
                INSERT INTO "_prisma_migrations" VALUES ('1', '20260524132142_test_1'), ('2', '20260524133555_v2'),
                                                        ('3', '20260525081137_v3');
                INSERT INTO "Product" (id, name, slug, price, category, "updatedAt")
                VALUES ('cprod1', 'Matalàs', 'matalas', 499.90, 'MATTRESS', now());""");
        return db;
    }

    private static void execute(DataSource ds, String sql) {
        try (Connection c = ds.getConnection()) {
            c.createStatement().execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String resource(String path) {
        try (InputStream in = PrismaBaselineTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
