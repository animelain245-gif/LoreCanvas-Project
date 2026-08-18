package com.lorecanvas.common

import java.time.Instant

/**
 * Logger (PEP-001, Phase 1 — Core Infrastructure: "Logging").
 *
 * Kept platform-agnostic on purpose: this module has no Android dependency,
 * so [PrintlnLogger] is a plain-JVM fallback (useful for unit tests and the
 * pure-Kotlin core modules). The `app` module provides an Android-specific
 * implementation backed by `android.util.Log` for real device logging —
 * every other layer only ever depends on this [Logger] interface, never on
 * a concrete implementation.
 */
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

interface Logger {
    fun debug(message: String, data: Any? = null)
    fun info(message: String, data: Any? = null)
    fun warn(message: String, data: Any? = null)
    fun error(message: String, data: Any? = null)
}

class PrintlnLogger(private val scope: String) : Logger {

    private fun write(level: LogLevel, message: String, data: Any?) {
        val prefix = "[${Instant.now()}] [$scope] [$level]"
        if (data != null) {
            println("$prefix $message $data")
        } else {
            println("$prefix $message")
        }
    }

    override fun debug(message: String, data: Any?) = write(LogLevel.DEBUG, message, data)
    override fun info(message: String, data: Any?) = write(LogLevel.INFO, message, data)
    override fun warn(message: String, data: Any?) = write(LogLevel.WARN, message, data)
    override fun error(message: String, data: Any?) = write(LogLevel.ERROR, message, data)
}

/** Factory so call sites read the same way as the TypeScript scaffold: `createLogger("Repository")`. */
fun createLogger(scope: String): Logger = PrintlnLogger(scope)
