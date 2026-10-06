package br.univesp.pi.expedientes.feature.auth

import app.cash.turbine.test
import br.univesp.pi.expedientes.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        var nextResult: AuthResult = AuthResult.Success
        var gate: CompletableDeferred<Unit>? = null
        val logins = mutableListOf<Pair<String, String>>()
        val registrations = mutableListOf<Triple<String, String, String>>()

        override suspend fun login(
            email: String,
            password: String,
        ): AuthResult {
            logins += email to password
            gate?.await()
            return nextResult
        }

        override suspend fun register(
            name: String,
            email: String,
            password: String,
        ): AuthResult {
            registrations += Triple(name, email, password)
            gate?.await()
            return nextResult
        }
    }

    private val repository = FakeAuthRepository()

    @Test
    fun invalidLoginDoesNotCallRepository() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = LoginViewModel(repository)
            viewModel.onChange(AuthField.Email, "nao-e-email")

            viewModel.submit()

            assertEquals(
                FieldError.Invalid(ValidationError.InvalidEmail),
                viewModel.state.value.fieldErrors[AuthField.Email],
            )
            assertEquals(
                FieldError.Invalid(ValidationError.BlankPassword),
                viewModel.state.value.fieldErrors[AuthField.Password],
            )
            assertTrue(repository.logins.isEmpty())
        }

    @Test
    fun editingFieldClearsItsError() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = LoginViewModel(repository)
            viewModel.submit()

            viewModel.onChange(AuthField.Email, "j")

            assertNull(viewModel.state.value.fieldErrors[AuthField.Email])
            assertEquals(
                FieldError.Invalid(ValidationError.BlankPassword),
                viewModel.state.value.fieldErrors[AuthField.Password],
            )
        }

    @Test
    fun loginSendsNormalizedEmailAndTogglesSubmitting() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate = CompletableDeferred<Unit>()
            repository.gate = gate
            val viewModel = LoginViewModel(repository)
            viewModel.onChange(AuthField.Email, " John@Example.com ")
            viewModel.onChange(AuthField.Password, "senha-segura-123")

            viewModel.state.test {
                assertFalse(awaitItem().isSubmitting)
                viewModel.submit()
                assertTrue(awaitItem().isSubmitting)
                gate.complete(Unit)
                assertFalse(awaitItem().isSubmitting)
            }
            assertEquals(listOf("john@example.com" to "senha-segura-123"), repository.logins)
        }

    @Test
    fun doubleSubmitSendsOnce() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.gate = CompletableDeferred()
            val viewModel = LoginViewModel(repository)
            viewModel.onChange(AuthField.Email, "john@example.com")
            viewModel.onChange(AuthField.Password, "x")

            viewModel.submit()
            viewModel.submit()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(1, repository.logins.size)
        }

    @Test
    fun loginFailureShowsGeneralError() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.nextResult = AuthResult.Failure(AuthFailure.Api("E-mail ou senha inválidos"))
            val viewModel = LoginViewModel(repository)
            viewModel.onChange(AuthField.Email, "john@example.com")
            viewModel.onChange(AuthField.Password, "errada")

            viewModel.submit()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(AuthFailure.Api("E-mail ou senha inválidos"), viewModel.state.value.generalError)
            assertFalse(viewModel.state.value.isSubmitting)
        }

    @Test
    fun registerValidatesAllFields() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = RegisterViewModel(repository)
            viewModel.onChange(AuthField.Password, "senha-segura-123")
            viewModel.onChange(AuthField.PasswordConfirm, "outra-senha-123")

            viewModel.submit()

            assertEquals(
                setOf(AuthField.Name, AuthField.Email, AuthField.PasswordConfirm),
                viewModel.state.value.fieldErrors.keys,
            )
            assertTrue(repository.registrations.isEmpty())
        }

    @Test
    fun registerSendsTrimmedValues() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = RegisterViewModel(repository)
            viewModel.onChange(AuthField.Name, "  John Doe ")
            viewModel.onChange(AuthField.Email, "JOHN@example.com")
            viewModel.onChange(AuthField.Password, "senha-segura-123")
            viewModel.onChange(AuthField.PasswordConfirm, "senha-segura-123")

            viewModel.submit()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(Triple("John Doe", "john@example.com", "senha-segura-123")), repository.registrations)
        }

    @Test
    fun duplicatedEmailGoesToEmailField() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.nextResult = AuthResult.Failure(AuthFailure.Api("E-mail já cadastrado", AuthField.Email))
            val viewModel = RegisterViewModel(repository)
            viewModel.onChange(AuthField.Name, "John")
            viewModel.onChange(AuthField.Email, "john@example.com")
            viewModel.onChange(AuthField.Password, "senha-segura-123")
            viewModel.onChange(AuthField.PasswordConfirm, "senha-segura-123")

            viewModel.submit()
            mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

            assertEquals(FieldError.Server("E-mail já cadastrado"), viewModel.state.value.fieldErrors[AuthField.Email])
            assertNull(viewModel.state.value.generalError)
        }
}
