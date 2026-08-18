package com.lorecanvas.repository

import com.lorecanvas.common.LcResult

/**
 * Transaction skeleton (PEP-001, Phase 1 — "Transaction skeleton").
 *
 * Implements the lifecycle from LCD-006, Chapter 11 exactly:
 *
 *   Begin Transaction -> Validate Request -> Execute Changes ->
 *   Update Repository State -> Commit
 *
 * and on failure:
 *
 *   Validation Failed -> Rollback -> Restore Previous State
 *
 * "The Repository must never leave the project in an intermediate state."
 * [Transaction.run] guarantees that: [execute] only ever runs after
 * [validate] succeeds, and if [execute] throws, [previousState] is what the
 * caller should restore — nothing is committed.
 */
data class TransactionFailure(val reason: String, val cause: Throwable? = null)

class Transaction<TState> {

    /**
     * Runs one transaction. [validate] returning [ValidationOutcome.Invalid]
     * or [execute] throwing both result in [LcResult.Fail] — the caller is
     * responsible for restoring [previousState] since this skeleton does not
     * assume how state is stored (in-memory object graph today, a real
     * database transaction after Phase 2's Room integration).
     */
    fun run(
        previousState: TState,
        validate: () -> ValidationOutcome,
        execute: (TState) -> TState
    ): LcResult<TState, TransactionFailure> {
        val outcome = validate()
        if (outcome is ValidationOutcome.Invalid) {
            return LcResult.fail(TransactionFailure("Validation failed: ${outcome.reason}"))
        }

        return try {
            val newState = execute(previousState)
            LcResult.ok(newState)
        } catch (e: Exception) {
            LcResult.fail(TransactionFailure("Execution failed, rolled back.", e))
        }
    }
}

sealed class ValidationOutcome {
    object Valid : ValidationOutcome()
    data class Invalid(val reason: String) : ValidationOutcome()
}
