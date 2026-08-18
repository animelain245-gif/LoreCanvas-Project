plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.lorecanvas.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lorecanvas.app"
        minSdk = 26 // Android 8.0 — covers the vast majority of active devices as of 2026.
        targetSdk = 36 // Google Play requires targeting API 36 (Android 16) for new apps from Aug 31, 2026.
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Shared, platform-independent business logic (Phase 1 scaffold).
    implementation(project(":core-common"))
    implementation(project(":core-domain"))
    implementation(project(":core-validation"))
    implementation(project(":core-events"))
    implementation(project(":core-commands"))
    implementation(project(":core-storage"))
    implementation(project(":core-repository"))
    implementation(project(":core-search"))
    implementation(project(":core-graph"))
    implementation(project(":core-plugin"))

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")

    // Coroutines — used by LoreCanvasApp.kt to run ProjectRepository's
    // (synchronous) file I/O off the main thread. Pinned to a version
    // contemporaneous with Kotlin 2.0.x rather than the newest release,
    // since this environment can't verify Maven dependency resolution —
    // a conservative, well-established pairing is the safer default here.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation(kotlin("test"))
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
