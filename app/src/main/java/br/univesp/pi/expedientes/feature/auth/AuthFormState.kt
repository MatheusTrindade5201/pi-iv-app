package br.univesp.pi.expedientes.feature.auth

sealed interface FieldError {
    data class Invalid(
        val error: ValidationError,
    ) : FieldError

    data class Server(
        val message: String,
    ) : FieldError
}

data class AuthFormState(
    val values: Map<AuthField, String> = emptyMap(),
    val fieldErrors: Map<AuthField, FieldError> = emptyMap(),
    val generalError: AuthFailure? = null,
    val isSubmitting: Boolean = false,
) {
    fun value(field: AuthField): String = values[field].orEmpty()

    fun edit(
        field: AuthField,
        value: String,
    ): AuthFormState =
        copy(
            values = values + (field to value),
            fieldErrors = fieldErrors - field,
            generalError = null,
        )

    fun withValidation(errors: Map<AuthField, ValidationError>): AuthFormState =
        copy(fieldErrors = errors.mapValues { FieldError.Invalid(it.value) }, generalError = null)

    fun withFailure(failure: AuthFailure): AuthFormState {
        val field = (failure as? AuthFailure.Api)?.field
        return if (failure is AuthFailure.Api && field != null) {
            copy(isSubmitting = false, fieldErrors = fieldErrors + (field to FieldError.Server(failure.message)))
        } else {
            copy(isSubmitting = false, generalError = failure)
        }
    }
}
