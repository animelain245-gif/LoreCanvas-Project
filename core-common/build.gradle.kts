plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Pure Kotlin/JVM module — no Android dependency by design, so this stays
// shareable with a future Kotlin Multiplatform desktop target.
dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
