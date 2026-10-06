package br.univesp.pi.expedientes.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {
    @Test
    fun normalizesEmail() {
        assertEquals("john@example.com", AuthValidation.normalizeEmail("  John@Example.COM "))
    }

    @Test
    fun validLoginHasNoErrors() {
        assertTrue(AuthValidation.validateLogin(" John@Example.com ", "x").isEmpty())
    }

    @Test
    fun loginRequiresValidEmailAndPassword() {
        assertEquals(
            mapOf(AuthField.Email to ValidationError.InvalidEmail, AuthField.Password to ValidationError.BlankPassword),
            AuthValidation.validateLogin("nao-e-email", ""),
        )
    }

    @Test
    fun invalidEmails() {
        listOf("", "john", "john@", "@example.com", "john@example", "jo hn@example.com").forEach {
            assertEquals(it, ValidationError.InvalidEmail, AuthValidation.validateLogin(it, "x")[AuthField.Email])
        }
    }

    @Test
    fun validRegisterHasNoErrors() {
        assertTrue(
            AuthValidation.validateRegister(" John Doe ", "john@example.com", "senha-123", "senha-123").isEmpty(),
        )
    }

    @Test
    fun registerRules() {
        assertEquals(
            mapOf(
                AuthField.Name to ValidationError.BlankName,
                AuthField.Email to ValidationError.InvalidEmail,
                AuthField.Password to ValidationError.ShortPassword,
                AuthField.PasswordConfirm to ValidationError.PasswordMismatch,
            ),
            AuthValidation.validateRegister("   ", "x", "curta", "outra"),
        )
    }

    @Test
    fun registerLimits() {
        val errors =
            AuthValidation.validateRegister(
                "a".repeat(121),
                "john@example.com",
                "s".repeat(129),
                "s".repeat(129),
            )

        assertEquals(ValidationError.LongName, errors[AuthField.Name])
        assertEquals(ValidationError.LongPassword, errors[AuthField.Password])
    }

    @Test
    fun passwordWithExactlyEightCharactersIsValid() {
        assertTrue(AuthValidation.validateRegister("John", "john@example.com", "12345678", "12345678").isEmpty())
    }
}
