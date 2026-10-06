package br.univesp.pi.expedientes.feature.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AuthScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginMatchesPrototype() {
        composeRule.setContent {
            ExpedientesTheme { LoginScreen(AuthFormState(), { _, _ -> }, {}, {}) }
        }

        listOf("Entre", "Email", "Senha", "exemplo@gmail.com", "Cadastrar-se", "Entrar").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun registerMatchesPrototype() {
        composeRule.setContent {
            ExpedientesTheme { RegisterScreen(AuthFormState(), { _, _ -> }, {}, {}) }
        }

        listOf("Cadastre", "Nome", "Nome Sobrenome", "Email", "Senha", "Confirmar Senha", "Cadastrar", "Entrar")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun typingAndSubmitReachCallbacks() {
        val typed = mutableListOf<Pair<AuthField, String>>()
        var submits = 0
        var registerClicks = 0
        composeRule.setContent {
            ExpedientesTheme {
                LoginScreen(
                    state = AuthFormState(),
                    onChange = { field, value -> typed += field to value },
                    onSubmit = { submits++ },
                    onRegisterClick = { registerClicks++ },
                )
            }
        }

        composeRule.onNodeWithTag("field_Email").performTextInput("john@example.com")
        composeRule.onNodeWithTag("submit").performClick()
        composeRule.onNodeWithText("Cadastrar-se").performClick()

        assertEquals(listOf(AuthField.Email to "john@example.com"), typed)
        assertEquals(1, submits)
        assertEquals(1, registerClicks)
    }

    @Test
    fun showsFieldAndGeneralErrors() {
        val state =
            mutableStateOf(
                AuthFormState(
                    fieldErrors = mapOf(AuthField.Email to FieldError.Invalid(ValidationError.InvalidEmail)),
                    generalError = AuthFailure.Network,
                ),
            )
        composeRule.setContent {
            ExpedientesTheme { LoginScreen(state.value, { _, _ -> }, {}, {}) }
        }

        composeRule.onNodeWithText("Informe um e-mail válido").assertIsDisplayed()
        composeRule
            .onNodeWithText("Sem conexão com o servidor. Verifique a internet e tente novamente.")
            .assertIsDisplayed()
    }

    @Test
    fun submitIsDisabledWhileSending() {
        composeRule.setContent {
            ExpedientesTheme { LoginScreen(AuthFormState(isSubmitting = true), { _, _ -> }, {}, {}) }
        }

        composeRule.onNodeWithTag("submit").assertIsNotEnabled()
    }
}
