package br.univesp.pi.expedientes.feature.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
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
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions =
        object : HomeActions {
            override fun retry() {
                calls += "retry"
            }

            override fun startShift() {
                calls += "start"
            }

            override fun chooseFuel(fuelId: String) {
                calls += "fuel:$fuelId"
            }

            override fun confirmFuel() {
                calls += "confirm"
            }

            override fun dismissStartFlow() {
                calls += "dismiss"
            }
        }

    private val today = LocalDate.of(2026, 9, 19)
    private val prototypeDay =
        DailyTotals(
            date = LocalDate.of(2026, 9, 18),
            shiftsCount = 1,
            earningsTotal = BigDecimal("245.00"),
            fuelCost = BigDecimal("57.60"),
            profit = BigDecimal("187.40"),
            distanceKm = BigDecimal("78.200"),
            durationMinutes = 240,
            idleMinutes = 30,
        )

    private fun show(
        state: HomeUiState,
        onOpenProfile: () -> Unit = {},
    ) {
        composeRule.setContent {
            ExpedientesTheme {
                HomeScreen(state, actions, onOpenProfile)
                StartShiftDialogs(state.startFlow, actions, onOpenProfile)
            }
        }
    }

    @Test
    fun idleMatchesPrototype() {
        show(HomeUiState(today = today, content = HomeContent.Idle("John Doe", prototypeDay)))

        listOf(
            "Sábado, 19 de Setembro",
            "Olá, John",
            "Último registro",
            "Ontem",
            "Lucro do dia",
            "+R$ 187,40",
            "R$ 245,00",
            "R$ 57,60",
            "78,2",
            "4h",
            "R$ 46,85",
            "2,40",
            "Iniciar expediente",
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }

        composeRule.onNodeWithTag("start_shift").performClick()
        assertEquals(listOf("start"), calls)
    }

    @Test
    fun idleWithoutRecords() {
        show(HomeUiState(today = today, content = HomeContent.Idle("John Doe", null)))

        composeRule.onNodeWithText("Nenhum expediente registrado ainda.").assertIsDisplayed()
    }

    @Test
    fun inProgressShowsPartialProfit() {
        val shift = ApiJson.decodeFromString(ShiftResponse.serializer(), Fixtures.read("shift_current"))
        show(HomeUiState(today = today, content = HomeContent.InProgress("John Doe", shift)))

        composeRule.onNodeWithText("Expediente em andamento").assertIsDisplayed()
        composeRule.onAllNodesWithText("4h").assertCountEquals(2)
        composeRule.onNodeWithText("Lucro Parcial").assertIsDisplayed()
        composeRule.onNodeWithTag("partial_profit").assertIsDisplayed()
        composeRule.onNodeWithText("+R$ 18,64").assertIsDisplayed()
    }

    @Test
    fun startingDisablesButton() {
        show(HomeUiState(today = today, content = HomeContent.Idle("John", null), startFlow = StartFlow.Checking))

        composeRule.onNodeWithTag("start_shift").assertIsNotEnabled()
    }

    @Test
    fun priceConflictOffersProfile() {
        var opened = 0
        show(
            HomeUiState(
                today = today,
                content = HomeContent.Idle("John", null),
                startFlow =
                    StartFlow.Error(RequestFailure.Api(409, "Cadastre o preço do combustível antes de iniciar"), true),
            ),
            onOpenProfile = { opened++ },
        )

        composeRule.onNodeWithText("Cadastre o preço do combustível antes de iniciar").assertIsDisplayed()
        composeRule.onNodeWithText("Ir para o Perfil").performClick()

        assertEquals(1, opened)
    }

    @Test
    fun noVehicleDialogLeadsToProfile() {
        var opened = 0
        show(
            HomeUiState(today = today, content = HomeContent.Idle("John", null), startFlow = StartFlow.NoVehicle),
            onOpenProfile = { opened++ },
        )

        composeRule.onNodeWithText("Cadastre sua moto").assertIsDisplayed()
        composeRule.onNodeWithText("Ir para o Perfil").performClick()

        assertEquals(1, opened)
        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun chooseFuelForm() {
        composeRule.setContent {
            ExpedientesTheme {
                ChooseFuelForm(
                    state = StartFlow.ChooseFuel("v1", FuelIds.ETHANOL),
                    onChoose = actions::chooseFuel,
                    onConfirm = actions::confirmFuel,
                )
            }
        }

        composeRule.onNodeWithText("Qual combustível você abasteceu?").assertIsDisplayed()
        composeRule.onNodeWithTag("chip_${FuelIds.ETHANOL}").assertIsSelected()
        composeRule.onNodeWithText("Gasolina").performClick()
        composeRule.onNodeWithTag("confirm_fuel").performClick()

        assertEquals(listOf("fuel:${FuelIds.GASOLINE}", "confirm"), calls)
    }
}
