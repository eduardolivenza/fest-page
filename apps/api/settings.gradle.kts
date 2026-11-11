rootProject.name = "festpage-api"

// Hexagonal layout: the compiler enforces the dependency rule (adapters -> application -> domain).
include(
    "domain",
    "application",
    "adapter-in-web",
    "adapter-out-persistence",
    "adapter-out-turnstile",
    "adapter-out-password",
    "bootstrap",
)

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}
