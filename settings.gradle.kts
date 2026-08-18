pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LoreCanvas"

// Pure-Kotlin modules (no Android dependency). These are the modules that
// can realistically be shared with a future Kotlin Multiplatform desktop
// target (LCD-016 successor for Phase "Desktop Port") without a rewrite.
include(":core-common")
include(":core-domain")
include(":core-validation")
include(":core-events")
include(":core-commands")
include(":core-storage")
include(":core-repository")
include(":core-search")
include(":core-graph")
include(":core-plugin")

// Android application module (Compose UI, Room, Android-specific adapters).
include(":app")
