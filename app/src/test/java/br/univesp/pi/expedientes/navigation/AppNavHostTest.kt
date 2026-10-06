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
                AppNavHost(isLoggedIn = loggedIn.value, homeContent = { Text("conteudo da home") })
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
    fun loggedInStartsOnHome() {
        launch(isLoggedIn = true)

        composeRule.onNodeWithText("conteudo da home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertIsSelected()
    }

    @Test
    fun enterOpensHomeTab() {
        launch(isLoggedIn = false)

        composeRule.onNodeWithText("Entrar").performClick()

        composeRule.onNodeWithText("conteudo da home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertIsSelected()
        composeRule.onNodeWithTag("tab_History").assertIsNotSelected()
    }

    @Test
    fun tabsSwitchScreens() {
        launch(isLoggedIn = true)

        composeRule.onNodeWithTag("tab_History").performClick()
        composeRule.onNodeWithTag("tab_History").assertIsSelected()
        composeRule.onNodeWithText("Em construção").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_Profile").performClick()
        composeRule.onNodeWithTag("tab_Profile").assertIsSelected()
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
