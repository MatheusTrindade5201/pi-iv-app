package br.univesp.pi.expedientes.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.PeriodSummary
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions =
        object : HistoryActions {
            override fun selectPeriod(days: Int) {
                calls += "period:$days"
            }

            override fun pullToRefresh() {
                calls += "pull"
            }

            override fun retry() {
                calls += "retry"
            }

            override fun toggleDay(date: LocalDate) {
                calls += "toggle:$date"
            }

            override fun startShift() {
                calls += "start"
            }
        }

    private val today = LocalDate.of(2026, 9, 20)
    private val summary = ApiJson.decodeFromString(PeriodSummary.serializer(), Fixtures.read("analytics_summary"))

    private fun day(
        date: LocalDate,
        profit: String = "187.40",
    ) = DailyTotals(
        date = date,
        shiftsCount = 1,
        earningsTotal = BigDecimal("245.00"),
        fuelCost = BigDecimal("57.60"),
        profit = BigDecimal(profit),
        distanceKm = BigDecimal("78.200"),
        durationMinutes = 240,
        idleMinutes = 30,
    )

    private val prototypeDays = (19 downTo 15).map { day(LocalDate.of(2026, 9, it)) }

    private fun show(
        state: HistoryUiState,
        onOpenHome: () -> Unit = { calls += "home" },
    ) {
        composeRule.setContent {
            ExpedientesTheme { HistoryScreen(state, actions, onOpenHome) }
        }
    }

    private fun loaded(
        days: List<DailyTotals> = prototypeDays,
        inProgress: Boolean = false,
        expanded: Set<LocalDate> = emptySet(),
    ) = HistoryUiState(
        today = today,
        content = HistoryContent.Loaded(HistoryData(days, summary, inProgress)),
        expanded = expanded,
    )

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag("history_list").performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun listMatchesPrototype() {
        show(loaded())

        composeRule.onNodeWithText("Histórico").assertIsDisplayed()
        composeRule.onNodeWithTag("chip_30").assertIsSelected()
        composeRule.onNodeWithTag("history_summary").assertIsDisplayed()
        listOf("Ontem", "18/09/2026", "17/09/2026", "16/09/2026", "15/09/2026").forEachIndexed { i, label ->
            val tag = "day_${prototypeDays[i].date}"
            scrollTo(tag)
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeRule.onNodeWithText("Iniciar expediente").assertIsDisplayed()
    }

    @Test
    fun summaryShowsPeriodTotals() {
        show(loaded())

        listOf("Resumo do período", "+R$ 18,64", "R$ 20,00", "R$ 1,36", "Dias trabalhados", "Média por dia")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun negativeDayIsShownInRed() {
        show(loaded(days = listOf(day(LocalDate.of(2026, 9, 19), profit = "-12.50"))))

        scrollTo("day_2026-09-19")
        composeRule.onNodeWithText("-R$ 12,50").assertIsDisplayed()
    }

    @Test
    fun tappingDayTogglesDetails() {
        show(loaded(days = listOf(day(today)), expanded = setOf(today)))

        scrollTo("day_$today")
        composeRule.onNodeWithText("Hoje").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 46,85").assertIsDisplayed()
        composeRule.onNodeWithTag("day_$today").performClick()

        assertEquals(listOf("toggle:$today"), calls)
    }

    @Test
    fun emptyPeriod() {
        show(loaded(days = emptyList()))

        composeRule.onNodeWithText("Nenhum expediente encerrado nos últimos 30 dias.").assertIsDisplayed()
        composeRule.onNodeWithTag("history_summary").assertDoesNotExist()
    }

    @Test
    fun startButtonAsksHomeToStart() {
        show(loaded())

        composeRule.onNodeWithTag("history_start").performClick()

        assertEquals(listOf("start", "home"), calls)
    }

    @Test
    fun runningShiftButtonOnlyOpensHome() {
        show(loaded(inProgress = true))

        composeRule.onNodeWithText("Ver expediente em andamento").performClick()

        assertEquals(listOf("home"), calls)
    }

    @Test
    fun periodChips() {
        show(loaded())

        composeRule.onNodeWithText("7 dias").performClick()

        assertEquals(listOf("period:7"), calls)
    }

    @Test
    fun failedStateRetries() {
        show(HistoryUiState(today = today, content = HistoryContent.Failed(RequestFailure.Network)))

        composeRule.onNodeWithText("Tentar de novo").performClick()

        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun refreshFailureKeepsList() {
        show(loaded().copy(refreshFailure = RequestFailure.Network))

        composeRule.onNodeWithText("Não foi possível atualizar", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("history_summary").assertIsDisplayed()
    }
}
