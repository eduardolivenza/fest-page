// Use cases (inbound ports + services) and outbound ports. Pure Java, no Spring.
plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":domain"))
    // In-memory implementations of the outbound ports, shared with the web adapter tests.
    testFixturesApi(project(":domain"))
}
