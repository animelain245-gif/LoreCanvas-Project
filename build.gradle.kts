// Top-level build file. Individual module build.gradle.kts files apply the
// plugins they need; this file only declares plugin versions once so every
// module stays in sync (avoids the classic "module A uses Kotlin 1.9.10,
// module B uses 1.9.22" drift).
plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
