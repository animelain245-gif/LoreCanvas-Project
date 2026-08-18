package com.lorecanvas.app

import android.util.Log
import com.lorecanvas.common.Logger

/**
 * Android-specific [Logger] implementation, backed by [android.util.Log].
 *
 * This is the only place in the codebase that touches `android.util.Log`
 * directly — every other layer (Repository, Domain, Validation, ...) only
 * ever depends on the platform-agnostic [Logger] interface from
 * `core-common`, so those modules stay reusable if a desktop target is
 * added later (per the roadmap).
 */
class AndroidLogger(private val scope: String) : Logger {
    override fun debug(message: String, data: Any?) {
        Log.d(scope, formatted(message, data))
    }

    override fun info(message: String, data: Any?) {
        Log.i(scope, formatted(message, data))
    }

    override fun warn(message: String, data: Any?) {
        Log.w(scope, formatted(message, data))
    }

    override fun error(message: String, data: Any?) {
        Log.e(scope, formatted(message, data))
    }

    private fun formatted(message: String, data: Any?): String =
        if (data != null) "$message | $data" else message
}
