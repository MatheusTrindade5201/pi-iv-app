package br.univesp.pi.expedientes.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import org.junit.Before
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

    @Before
    fun setUp() {
        composeRule.setContent {
            ExpedientesTheme {
                AppNavHost(homeContent = { Text("conteudo da home") })
            }
        }
    }

    @Test
    fun startsOnLoginWithoutBottomBar() {
        composeRule.onNodeWithText("Entre").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertDoesNotExist()
    }

    @Test
    fun enterOpensHomeTab() {
        composeRule.onNodeWithText("Entrar").performClick()

        composeRule.onNodeWithText("conteudo da home").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_Home").assertIsSelected()
        composeRule.onNodeWithTag("tab_History").assertIsNotSelected()
    }

    @Test
    fun tabsSwitchScreens() {
        composeRule.onNodeWithText("Entrar").performClick()

        composeRule.onNodeWithTag("tab_History").performClick()
        composeRule.onNodeWithTag("tab_History").assertIsSelected()
        composeRule.onNodeWithText("Em construção").assertIsDisplayed()

        composeRule.onNodeWithTag("tab_Profile").performClick()
        composeRule.onNodeWithTag("tab_Profile").assertIsSelected()
        composeRule.onNodeWithTag("tab_Home").assertIsNotSelected()

        composeRule.onNodeWithTag("tab_Home").performClick()
        composeRule.onNodeWithText("conteudo da home").assertIsDisplayed()
    }
}
