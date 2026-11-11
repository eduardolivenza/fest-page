package com.festdescans.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.festdescans.api.adapter.out.persistence.EmbeddedPostgresDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

/** The initContainer entry point: migrates with the app's configuration, needs no JWT_SECRET, exits. */
class MigrateDatabaseTest {

    @Test
    void migratesAnEmptyDatabaseAndIsIdempotent() {
        EmbeddedPostgresDatabase db = EmbeddedPostgresDatabase.create();
        String[] args = {"--spring.datasource.url=" + db.jdbcUrl(), "--spring.datasource.username=" + db.username(),
                "--spring.datasource.password=" + db.password()};

        MigrateDatabase.main(args);
        MigrateDatabase.main(args);

        assertThat(JdbcClient.create(db.dataSource()).sql("select version from flyway_schema_history order by installed_rank")
                .query(String.class).list()).containsExactly("1", "2", "3");
    }

    @Test
    void failsWhenTheDatabaseIsUnreachable() {
        assertThatThrownBy(() -> MigrateDatabase.main(new String[] {
                "--spring.datasource.url=jdbc:postgresql://127.0.0.1:1/nope",
                "--spring.datasource.hikari.connection-timeout=1000"}))
                .isInstanceOf(RuntimeException.class);
    }
}
