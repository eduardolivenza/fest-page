// Driven adapter: PostgreSQL through Spring Data JPA, plus the Flyway migrations that own the schema.
plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(project(":application"))
    implementation(libs.spring.boot.starter.data.jpa)
    // Boot's Flyway auto-configuration runs db/migration on startup, before Hibernate.
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    // Real PostgreSQL (same version as production) without Docker, shared with the bootstrap tests.
    testFixturesApi(libs.embedded.postgres)
    testFixturesApi(platform(libs.postgres.binaries.bom))
    testFixturesImplementation(platform(libs.spring.boot.bom))
    testFixturesRuntimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
}
