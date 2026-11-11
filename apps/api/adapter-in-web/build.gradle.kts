// Driving adapter: REST controllers, request validation, error mapping, JWT security, CORS, OpenAPI.
dependencies {
    implementation(project(":application"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.springdoc.webmvc.ui)

    testImplementation(testFixtures(project(":application")))
    testImplementation(project(":adapter-out-password"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.security.test)
}
