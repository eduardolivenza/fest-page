// FestPage api: one Spring Boot module; the hexagonal layers are packages (see HexagonalArchitectureTest).
// Versions come from gradle/libs.versions.toml; Spring Boot's BOM manages the rest.
plugins {
    java
    alias(libs.plugins.spring.boot)
}

group = "com.festdescans"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.springdoc.webmvc.ui)
    implementation(libs.flyway.postgresql)
    implementation(libs.bouncycastle)
    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.spring.boot.bom))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.archunit)
    // Real PostgreSQL (same version as production) without Docker.
    testImplementation(libs.embedded.postgres)
    testImplementation(platform(libs.postgres.binaries.bom))
    testRuntimeOnly(libs.junit.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // -parameters: Spring binds @PathVariable/@RequestParam by parameter name.
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
    // UTC+14: a timestamp shifted by the JVM's zone fails the tests, also on UTC CI runners.
    systemProperty("user.timezone", "Pacific/Kiritimati")
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

springBoot {
    mainClass = "com.festdescans.api.FestPageApplication"
}

tasks.bootJar {
    archiveFileName = "app.jar"
}

// Only the Boot jar is shipped.
tasks.jar {
    enabled = false
}

// Local dev (`./gradlew bootRun`): a dev-only JWT secret unless one is exported.
tasks.bootRun {
    environment("JWT_SECRET", System.getenv("JWT_SECRET") ?: "dev-only-jwt-secret-not-for-production-0123")
}
