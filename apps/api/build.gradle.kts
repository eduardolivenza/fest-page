// Conventions shared by every module. Versions come from gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.spring.boot) apply false
}

val javaVersion = libs.versions.java.get().toInt()
val springBootBom = libs.spring.boot.bom
val junitJupiter = libs.junit.jupiter
val junitLauncher = libs.junit.launcher
val assertj = libs.assertj

subprojects {
    apply(plugin = "java-library")

    group = "com.festdescans"

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    dependencies {
        // Spring Boot's BOM manages every version not pinned in the catalog.
        "implementation"(platform(springBootBom))
        "testImplementation"(platform(springBootBom))
        "testImplementation"(junitJupiter)
        "testImplementation"(assertj)
        "testRuntimeOnly"(junitLauncher)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // -parameters: Spring binds @PathVariable/@RequestParam by parameter name.
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial", "-Werror"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // The real-database tests start an embedded PostgreSQL; keep them in one JVM.
        maxParallelForks = 1
        // UTC+14: a timestamp shifted by the JVM's zone fails the tests, also on UTC CI runners.
        systemProperty("user.timezone", "Pacific/Kiritimati")
        testLogging {
            events("failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
