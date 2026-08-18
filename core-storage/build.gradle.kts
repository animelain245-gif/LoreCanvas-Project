plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Phase 1 scope: interface only (LCD-007's real storage format/engine is a
// Phase 2 "Project System" deliverable per PEP-001, not Phase 1). Kept as
// its own module now so the future Room-backed implementation can live in
// an Android-specific module without this interface module ever changing.
dependencies {
    implementation(project(":core-common"))
    implementation(project(":core-domain"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
