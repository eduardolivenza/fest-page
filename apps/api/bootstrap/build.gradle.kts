// Spring Boot application: wires use cases to adapters and produces the runnable jar.
plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":application"))
    implementation(project(":adapter-in-web"))
    implementation(project(":adapter-out-persistence"))
    implementation(project(":adapter-out-turnstile"))
    implementation(project(":adapter-out-password"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.flyway.core)

    testImplementation(testFixtures(project(":adapter-out-persistence")))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
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
