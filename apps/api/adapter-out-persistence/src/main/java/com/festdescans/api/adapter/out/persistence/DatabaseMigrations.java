package com.festdescans.api.adapter.out.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Flyway owns the schema. V1-V3 are the three Prisma migrations, copied unchanged.
 *
 * <p>A database created by Prisma (production) has the tables but no Flyway history: Flyway then records a
 * baseline at version 3 and runs nothing. Before that, the Prisma history is checked: all three migrations,
 * and only those, must have been applied, or the baseline would hide a missing migration.
 * An empty database (CI, local dev) runs V1-V3.
 */
public final class DatabaseMigrations {

    static final String BASELINE_VERSION = "3";
    static final Set<String> PRISMA_MIGRATIONS = Set.of(
            "20260524132142_test_1",
            "20260524133555_v2",
            "20260525081137_v3");

    private DatabaseMigrations() {
    }

    public static MigrateResult migrate(DataSource dataSource) {
        return migrate(Flyway.configure().dataSource(dataSource));
    }

    /** Used by the migrate initContainer, which has no Spring context. */
    public static MigrateResult migrate(String url, String user, String password) {
        return migrate(Flyway.configure().dataSource(url, user, password));
    }

    private static MigrateResult migrate(FluentConfiguration config) {
        Flyway flyway = config
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(BASELINE_VERSION)
                .baselineDescription("Prisma migrations " + String.join(", ", PRISMA_MIGRATIONS.stream().sorted().toList()))
                .load();
        checkPrismaHistoryBeforeBaseline(flyway.getConfiguration().getDataSource());
        return flyway.migrate();
    }

    static void checkPrismaHistoryBeforeBaseline(DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            if (tableExists(c, "flyway_schema_history") || !hasTables(c)) {
                return; // already managed by Flyway, or a fresh database
            }
            if (!tableExists(c, "_prisma_migrations")) {
                throw new IllegalStateException("The database has tables but neither a Flyway nor a Prisma history: "
                        + "refusing to baseline it. Check which schema DATABASE_URL points to.");
            }
            Set<String> applied = new HashSet<>();
            try (ResultSet rs = c.createStatement().executeQuery(
                    "select migration_name from _prisma_migrations where finished_at is not null and rolled_back_at is null")) {
                while (rs.next()) {
                    applied.add(rs.getString(1));
                }
            }
            if (!applied.equals(PRISMA_MIGRATIONS)) {
                throw new IllegalStateException("Prisma migrations applied " + applied.stream().sorted().toList()
                        + " but the Flyway baseline (V" + BASELINE_VERSION + ") expects exactly "
                        + PRISMA_MIGRATIONS.stream().sorted().toList()
                        + ". Bring the database up to date with the previous (Node) api image first.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not inspect the database before migrating", e);
        }
    }

    private static boolean tableExists(Connection c, String table) throws SQLException {
        try (var ps = c.prepareStatement(
                "select 1 from information_schema.tables where table_schema = current_schema() and table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean hasTables(Connection c) throws SQLException {
        try (ResultSet rs = c.createStatement().executeQuery(
                "select 1 from information_schema.tables where table_schema = current_schema() limit 1")) {
            return rs.next();
        }
    }
}
