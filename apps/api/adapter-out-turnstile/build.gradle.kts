// Driven adapter: Cloudflare Turnstile verification over the JDK HTTP client.
dependencies {
    implementation(project(":application"))
    implementation(libs.jackson.databind)
}
