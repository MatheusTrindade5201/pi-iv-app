package br.univesp.pi.expedientes.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.feature.auth.AuthFormState
import br.univesp.pi.expedientes.feature.auth.LoginScreen
import br.univesp.pi.expedientes.feature.auth.RegisterScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppNavHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val loggedIn = mutableStateOf(false)

    private fun launch(isLoggedIn: Boolean) {
        loggedIn.value = isLoggedIn
        composeRule.setContent {
            ExpedientesTheme {
                AppNavHost(
                    isLoggedIn = loggedIn.value,
                    loginContent = { onRegister -> LoginScreen(AuthFormState(), { _, _ -> }, {}, onRegister) },
                    registerContent = { onLogin -> RegisterScreen(AuthFormState(), { _, _ -> }, {}, onLogin) },
                    homeContent = { Text("conteudo da home") },
                    profileContent = { Text("conteudo do perfil") },
                )
            }
        }
    }

    @Test
    fun loggedOutStartsOnLoginWithoutBottomBar() {
        launch(isLoggedIn = false)

        composeRule.onNodeWithText("Entre").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertDoesNotExist()
    }

    @Test
    fun loginLinksToRegisterAndBack() {
        launch(isLoggedIn = false)

        composeRule.onNodeWithText("Cadastrar-se").performClick()
        composeRule.onNodeWithText("Cadastre").assertIsDisplayed()

        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.onNodeWithText("Entre").assertIsDisplayed()
    }

    @Test
    fun sessionStartedOnLoginOpensHome() {
        launch(isLoggedIn = false)

        loggedIn.value = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("conteudo da home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertIsSelected()
    }

    @Test
    fun sessionStartedOnRegisterOpensHome() {
        launch(isLoggedIn = false)
        composeRule.onNodeWithText("Cadastrar-se").performClick()

        loggedIn.value = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText("conteudo da home").assertIsDisplayed()
    }

    @Test
    fun loggedInStartsOnHome() {
        launch(isLoggedIn = true)

        composeRule.onNodeWithText("conteudo da home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertIsSelected()
    }

    @Test
    fun tabsSwitchScreens() {
        launch(isLoggedIn = true)

        composeRule.onNodeWithTag("tab_History").performClick()
        composeRule.onNodeWithTag("tab_History").assertIsSelected()
        composeRule.onNodeWithText("Em construção").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_Profile").performClick()
        composeRule.onNodeWithTag("tab_Profile").assertIsSelected()
        composeRule.onNodeWithText("conteudo do perfil").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertIsNotSelected()

        composeRule.onNodeWithTag("tab_Home").performClick()
        composeRule.onNodeWithText("conteudo da home").assertIsDisplayed()
    }

    @Test
    fun logoutReturnsToLoginFromAnyTab() {
        launch(isLoggedIn = true)
        composeRule.onNodeWithTag("tab_Profile").performClick()

        loggedIn.value = false
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Entre").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertDoesNotExist()
    }
}
