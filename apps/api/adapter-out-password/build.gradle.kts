// Driven adapter: Argon2id password hashes (PHC strings, compatible with the node-argon2 hashes in the database).
dependencies {
    implementation(project(":application"))
    implementation(libs.bouncycastle)
}
