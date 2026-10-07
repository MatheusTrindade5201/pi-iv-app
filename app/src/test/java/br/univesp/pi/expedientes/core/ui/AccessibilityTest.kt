package br.univesp.pi.expedientes.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.PeriodSummary
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.feature.history.HistoryActions
import br.univesp.pi.expedientes.feature.history.HistoryContent
import br.univesp.pi.expedientes.feature.history.HistoryData
import br.univesp.pi.expedientes.feature.history.HistoryScreen
import br.univesp.pi.expedientes.feature.history.HistoryUiState
import br.univesp.pi.expedientes.feature.home.FinishFlow
import br.univesp.pi.expedientes.feature.home.FinishForm
import br.univesp.pi.expedientes.feature.home.HomeActions
import br.univesp.pi.expedientes.feature.home.HomeContent
import br.univesp.pi.expedientes.feature.home.HomeScreen
import br.univesp.pi.expedientes.feature.home.HomeUiState
import br.univesp.pi.expedientes.feature.home.ShiftActions
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val homeActions =
        object : HomeActions {
            override fun retry() = Unit

            override fun startShift() = Unit

            override fun chooseFuel(fuelId: String) = Unit

            override fun confirmFuel() = Unit

            override fun dismissStartFlow() = Unit
        }

    private val shiftActions =
        object : ShiftActions {
            override fun openEarning() = Unit

            override fun changeEarning(text: String) = Unit

            override fun choosePreset(value: BigDecimal) = Unit

            override fun addEarning() = Unit

            override fun overwriteEarning() = Unit

            override fun dismissEarning() = Unit

            override fun openFinish() = Unit

            override fun confirmFinish() = Unit

            override fun dismissFinish() = Unit

            override fun resumeTracking() = Unit
        }

    private val historyActions =
        object : HistoryActions {
            override fun selectPeriod(days: Int) = Unit

            override fun pullToRefresh() = Unit

            override fun retry() = Unit

            override fun toggleDay(date: LocalDate) = Unit

            override fun startShift() = Unit
        }

    private val shift = ApiJson.decodeFromString(ShiftResponse.serializer(), Fixtures.read("shift_current"))
    private val days =
        ApiJson.decodeFromString(ListSerializer(DailyTotals.serializer()), Fixtures.read("analytics_daily"))
    private val summary = ApiJson.decodeFromString(PeriodSummary.serializer(), Fixtures.read("analytics_summary"))
    private val today = LocalDate.of(2026, 10, 6)

    private fun showLarge(content: @Composable () -> Unit) {
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                ExpedientesTheme { content() }
            }
        }
    }

    @Test
    fun idleHomeKeepsStartButtonAtDoubleFont() {
        showLarge {
            HomeScreen(
                HomeUiState(today = today, content = HomeContent.Idle("John Doe", days.first())),
                homeActions,
                shiftActions,
                shift.startedAt,
                {},
            )
        }

        composeRule.onNodeWithTag("start_shift").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun inProgressHomeKeepsActionsAtDoubleFont() {
        showLarge {
            HomeScreen(
                HomeUiState(today = today, content = HomeContent.InProgress("John Doe", shift), gpsActive = true),
                homeActions,
                shiftActions,
                shift.gps.lastPointAt!!,
                {},
            )
        }

        composeRule.onNodeWithTag("open_earning").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("open_finish").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("partial_profit").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun finishSheetScrollsToConfirmAtDoubleFont() {
        showLarge { FinishForm(FinishFlow(), shift, 222, 6, shiftActions) }

        composeRule.onNodeWithTag("confirm_finish").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun historyAtDoubleFontWithHeadingAndDayState() {
        showLarge {
            HistoryScreen(
                HistoryUiState(
                    today = today,
                    content = HistoryContent.Loaded(HistoryData(days, summary, shiftInProgress = false)),
                ),
                historyActions,
                {},
            )
        }

        composeRule.onNodeWithTag("history_start").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("chip_7").assertHeightIsAtLeast(48.dp)
        composeRule.onAllNodes(isHeading())[0].assertTextEquals("Histórico")
        composeRule.onNodeWithTag("history_list").performScrollToNode(hasTestTag("day_$today"))
        composeRule
            .onNodeWithTag("day_$today")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "detalhes fechados"))
    }

    @Test
    fun loadingIsAnnounced() {
        showLarge {
            HistoryScreen(HistoryUiState(today = today), historyActions, {})
        }

        composeRule.onNodeWithContentDescription("Carregando").assertIsDisplayed()
    }
}
