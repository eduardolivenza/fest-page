package com.festdescans.api.adapter.out.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

/**
 * The production database was created by Prisma: it has the tables but no Flyway history, so Flyway
 * records a baseline at V3 (spring.flyway.baseline-on-migrate) and runs nothing. Before that happens this
 * checks that Prisma applied exactly the three migrations that V1-V3 copy; otherwise the baseline would hide
 * a missing or unknown migration.
 */
final class PrismaBaselineGuard {

    static final Set<String> PRISMA_MIGRATIONS = Set.of(
            "20260524132142_test_1",
            "20260524133555_v2",
            "20260525081137_v3");

    private PrismaBaselineGuard() {
    }

    static void check(DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            if (tableExists(c, "flyway_schema_history") || !hasTables(c)) {
                return; // already managed by Flyway, or a fresh database (V1-V3 will run)
            }
            if (!tableExists(c, "_prisma_migrations")) {
                throw new IllegalStateException("The database has tables but neither a Flyway nor a Prisma history: "
                        + "refusing to baseline it. Check which database spring.datasource.url points to.");
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
                        + " but the Flyway baseline (V3) expects exactly " + PRISMA_MIGRATIONS.stream().sorted().toList()
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
