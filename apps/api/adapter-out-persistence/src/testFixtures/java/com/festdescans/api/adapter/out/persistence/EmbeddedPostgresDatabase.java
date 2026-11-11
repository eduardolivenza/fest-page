package com.festdescans.api.adapter.out.persistence;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

/**
 * One real PostgreSQL server per test JVM (zonky embedded binaries, same version as production, no
 * Docker needed). Each caller gets its own empty database.
 */
public final class EmbeddedPostgresDatabase {

    private static final AtomicInteger NEXT = new AtomicInteger();
    private static EmbeddedPostgres server;

    private final String name;

    private EmbeddedPostgresDatabase(String name) {
        this.name = name;
    }

    public static synchronized EmbeddedPostgresDatabase create() {
        if (server == null) {
            try {
                server = EmbeddedPostgres.builder().start();
            } catch (IOException e) {
                throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
            }
            EmbeddedPostgres started = server;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    started.close();
                } catch (IOException ignored) {
                    // JVM is exiting
                }
            }));
        }
        String name = "festpage_test_" + NEXT.incrementAndGet();
        try (Connection c = server.getPostgresDatabase().getConnection()) {
            c.createStatement().execute("create database " + name);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return new EmbeddedPostgresDatabase(name);
    }

    public String jdbcUrl() {
        return server.getJdbcUrl("postgres", name);
    }

    public String username() {
        return "postgres";
    }

    public String password() {
        return "";
    }

    public DataSource dataSource() {
        return server.getDatabase("postgres", name);
    }

    public String serverVersion() {
        try (Connection c = dataSource().getConnection()) {
            return c.getMetaData().getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
