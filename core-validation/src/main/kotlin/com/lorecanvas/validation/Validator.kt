package com.lorecanvas.validation

/**
 * Validation skeleton (PEP-001, Phase 1 — "Validation skeleton").
 *
 * Every write that reaches the Repository passes through a Validator before
 * touching Storage (LCD-006, LCD-009). Phase 1 defines the contract only;
 * concrete rule sets (Node name rules, Relationship type rules, etc.) are
 * added alongside the phases that introduce those entities.
 */
data class ValidationError(val field: String, val message: String)

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()

    val isValid: Boolean get() = this is Valid
}

fun interface Validator<in T> {
    fun validate(value: T): ValidationResult
}

/**
 * Combines multiple validators for the same type into one, collecting every
 * failure instead of stopping at the first (so the UI can show all problems
 * at once rather than one-at-a-time).
 */
class CompositeValidator<T>(private val validators: List<Validator<T>>) : Validator<T> {
    override fun validate(value: T): ValidationResult {
        val errors = validators
            .map { it.validate(value) }
            .filterIsInstance<ValidationResult.Invalid>()
            .flatMap { it.errors }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
