package com.lorecanvas.common

/**
 * LcResult<T, E> — a shared "common utility" (PEP-001, Phase 1).
 *
 * Named `LcResult` (not `Result`) to avoid colliding with Kotlin's built-in
 * `kotlin.Result`, which is exception-oriented. Repository, Validation, and
 * Storage operations fail in expected ways (validation failure, not found,
 * IO error) — this gives every layer one consistent, typed way to express
 * that without throwing for control flow.
 */
sealed class LcResult<out T, out E> {
    data class Ok<out T>(val value: T) : LcResult<T, Nothing>()
    data class Fail<out E>(val error: E) : LcResult<Nothing, E>()

    val isOk: Boolean get() = this is Ok
    val isFail: Boolean get() = this is Fail

    inline fun <R> fold(onOk: (T) -> R, onFail: (E) -> R): R = when (this) {
        is Ok -> onOk(value)
        is Fail -> onFail(error)
    }

    companion object {
        fun <T> ok(value: T): LcResult<T, Nothing> = Ok(value)
        fun <E> fail(error: E): LcResult<Nothing, E> = Fail(error)
    }
}
