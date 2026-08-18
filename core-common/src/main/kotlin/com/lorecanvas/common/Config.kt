package com.lorecanvas.common

/**
 * Configuration (PEP-001, Phase 1 — Core Infrastructure: "Configuration").
 *
 * Deliberately tiny for Phase 1. Android-specific config (BuildConfig fields,
 * app data directory, etc.) is resolved in the `app` module and passed in
 * here rather than read directly, keeping this module free of any Android
 * dependency.
 */
enum class AppEnvironment { DEVELOPMENT, PRODUCTION, TEST }

data class AppConfig(
    val environment: AppEnvironment,
    val appName: String = "LoreCanvas",
    val appVersion: String = "0.1.0"
)
