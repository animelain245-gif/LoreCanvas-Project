package com.lorecanvas.validation

/**
 * Small library of reusable field-level rules. Entity-specific validators
 * (introduced in later phases) compose these rather than reimplementing
 * basic checks like "not blank" repeatedly.
 */
object Rules {

    fun notBlank(field: String, value: String): ValidationResult =
        if (value.isBlank()) {
            ValidationResult.Invalid(listOf(ValidationError(field, "$field must not be empty.")))
        } else {
            ValidationResult.Valid
        }

    fun maxLength(field: String, value: String, max: Int): ValidationResult =
        if (value.length > max) {
            ValidationResult.Invalid(listOf(ValidationError(field, "$field must be at most $max characters.")))
        } else {
            ValidationResult.Valid
        }

    fun isValidUuid(field: String, value: String): ValidationResult =
        if (com.lorecanvas.common.UuidService.isValid(value)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(listOf(ValidationError(field, "$field must be a valid UUID.")))
        }
}
