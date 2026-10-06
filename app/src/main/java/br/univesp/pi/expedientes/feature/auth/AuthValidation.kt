package br.univesp.pi.expedientes.feature.auth

enum class AuthField {
    Name,
    Email,
    Password,
    PasswordConfirm,
}

enum class ValidationError {
    BlankName,
    LongName,
    InvalidEmail,
    BlankPassword,
    ShortPassword,
    LongPassword,
    PasswordMismatch,
}

object AuthValidation {
    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 128
    const val MAX_NAME = 120

    private val email = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(value: String): String = value.trim().lowercase()

    fun validateLogin(
        email: String,
        password: String,
    ): Map<AuthField, ValidationError> =
        buildMap {
            emailError(email)?.let { put(AuthField.Email, it) }
            if (password.isEmpty()) put(AuthField.Password, ValidationError.BlankPassword)
        }

    fun validateRegister(
        name: String,
        email: String,
        password: String,
        passwordConfirm: String,
    ): Map<AuthField, ValidationError> =
        buildMap {
            val trimmedName = name.trim()
            when {
                trimmedName.isEmpty() -> put(AuthField.Name, ValidationError.BlankName)
                trimmedName.length > MAX_NAME -> put(AuthField.Name, ValidationError.LongName)
            }
            emailError(email)?.let { put(AuthField.Email, it) }
            when {
                password.length < MIN_PASSWORD -> put(AuthField.Password, ValidationError.ShortPassword)
                password.length > MAX_PASSWORD -> put(AuthField.Password, ValidationError.LongPassword)
            }
            if (passwordConfirm != password) put(AuthField.PasswordConfirm, ValidationError.PasswordMismatch)
        }

    private fun emailError(value: String): ValidationError? =
        if (email.matches(normalizeEmail(value))) null else ValidationError.InvalidEmail
}
