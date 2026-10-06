package br.univesp.pi.expedientes.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.EarningResponse
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.feature.tracking.PermissionPrompt
import br.univesp.pi.expedientes.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.Instant
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

    private val shiftActions =
        object : ShiftActions {
            override fun openEarning() {
                calls += "openEarning"
            }

            override fun changeEarning(text: String) {
                calls += "change:$text"
            }

            override fun choosePreset(value: BigDecimal) {
                calls += "preset:$value"
            }

            override fun addEarning() {
                calls += "add"
            }

            override fun overwriteEarning() {
                calls += "overwrite"
            }

            override fun dismissEarning() {
                calls += "dismissEarning"
            }

            override fun openFinish() {
                calls += "openFinish"
            }

            override fun confirmFinish() {
                calls += "confirmFinish"
            }

            override fun dismissFinish() {
                calls += "continue"
            }

            override fun resumeTracking() {
                calls += "resume"
            }
        }

    private val today = LocalDate.of(2026, 9, 19)
    private val shift = ApiJson.decodeFromString(ShiftResponse.serializer(), Fixtures.read("shift_current"))
    private val lastPoint = shift.gps.lastPointAt!!
    private val prototypeShift =
        shift.copy(
            summary =
                shift.summary.copy(
                    earningsTotal = BigDecimal("245.00"),
                    fuelCost = BigDecimal("57.60"),
                    profit = BigDecimal("187.40"),
                    distanceKm = BigDecimal("78.200"),
                    durationMinutes = 240,
                    profitPerHour = BigDecimal("46.85"),
                    profitPerKm = BigDecimal("2.39"),
                ),
        )
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
        now: Instant = lastPoint.plusSeconds(4),
    ) {
        composeRule.setContent {
            ExpedientesTheme {
                HomeScreen(state, actions, shiftActions, now, onOpenProfile)
                StartShiftDialogs(state.startFlow, actions, onOpenProfile)
            }
        }
    }

    private fun inProgress(
        gpsActive: Boolean = true,
        shift: ShiftResponse = prototypeShift,
    ) = HomeUiState(today = today, content = HomeContent.InProgress("John Doe", shift), gpsActive = gpsActive)

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
    fun inProgressMatchesPrototype() {
        show(inProgress(), now = shift.startedAt.plusSeconds(222 * 60))

        listOf(
            "Expediente em andamento",
            "Lucro Parcial",
            "+R$ 187,40",
            "R$ 245,00",
            "R$ 57,60",
            "78,2",
            "4h",
            "R$ 46,85",
            "2,39",
            "Lançar ganho",
            "Encerrar Expediente",
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }

        composeRule.onNodeWithTag("open_earning").performClick()
        composeRule.onNodeWithTag("open_finish").performClick()
        assertEquals(listOf("openEarning", "openFinish"), calls)
    }

    @Test
    fun elapsedTimeFollowsTheClock() {
        show(inProgress(), now = shift.startedAt.plusSeconds(222 * 60 + 30))

        composeRule.onNodeWithTag("elapsed").assertTextEquals("3h 42min")
    }

    @Test
    fun gpsActiveBannerShowsSecondsSinceLastPoint() {
        show(inProgress())

        composeRule.onNodeWithText("GPS ativo - último ponto há 4s").assertIsDisplayed()
        composeRule.onNodeWithText("Ativar").assertDoesNotExist()
    }

    @Test
    fun gpsWaitingForFirstPoint() {
        show(inProgress(shift = shift.copy(gps = shift.gps.copy(lastPointAt = null))))

        composeRule.onNodeWithText("GPS ativo - aguardando sinal").assertIsDisplayed()
    }

    @Test
    fun gpsOffOffersToResume() {
        show(inProgress(gpsActive = false), now = lastPoint.plusSeconds(6 * 60))

        composeRule.onNodeWithText("GPS inativo - último ponto há 6 min").assertIsDisplayed()
        composeRule.onNodeWithText("Ativar").performClick()

        assertEquals(listOf("resume"), calls)
    }

    @Test
    fun earningForm() {
        composeRule.setContent {
            ExpedientesTheme {
                EarningForm(EarningFlow(text = "20,00", lastEarning = null), shiftActions)
            }
        }

        composeRule.onNodeWithTag("earning_amount").assertTextEquals("R$ 20,00")
        composeRule.onNodeWithTag("chip_20").assertIsSelected()
        composeRule.onNodeWithTag("overwrite_earning").assertIsNotEnabled()
        composeRule.onNodeWithText("R$ 5,00").performClick()
        composeRule.onNodeWithTag("add_earning").performClick()

        assertEquals(listOf("preset:5", "add"), calls)
    }

    @Test
    fun earningFormWithLastEarningAllowsOverwrite() {
        val last = ApiJson.decodeFromString(EarningResponse.serializer(), Fixtures.read("earning"))
        composeRule.setContent {
            ExpedientesTheme {
                EarningForm(EarningFlow(text = "8", lastEarning = last), shiftActions)
            }
        }

        composeRule.onNodeWithText("Último lançado: R$ 20,00").assertIsDisplayed()
        composeRule.onNodeWithTag("chip_8").assertIsSelected()
        composeRule.onNodeWithTag("overwrite_earning").performClick()

        assertEquals(listOf("overwrite"), calls)
    }

    @Test
    fun emptyEarningCannotBeSent() {
        composeRule.setContent {
            ExpedientesTheme { EarningForm(EarningFlow(), shiftActions) }
        }

        composeRule.onNodeWithTag("earning_amount").assertTextEquals("R$ 0,00")
        composeRule.onNodeWithTag("add_earning").assertIsNotEnabled()
    }

    @Test
    fun finishFormMatchesPrototype() {
        composeRule.setContent {
            ExpedientesTheme {
                FinishForm(
                    FinishFlow(),
                    prototypeShift,
                    elapsedMinutes = 222,
                    inactiveMinutes = 6,
                    actions = shiftActions,
                )
            }
        }

        listOf("Encerrar expediente", "Tempo percorrido", "3h 42min", "GPS ficou inativo por 6 min", "+R$ 187,40")
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithTag("confirm_finish").performClick()

        assertEquals(listOf("continue", "confirmFinish"), calls)
    }

    @Test
    fun finishFormWithoutGapsOrWhileFinishing() {
        composeRule.setContent {
            ExpedientesTheme {
                FinishForm(
                    FinishFlow(finishing = true, failure = RequestFailure.Network),
                    prototypeShift,
                    elapsedMinutes = 10,
                    inactiveMinutes = 0,
                    actions = shiftActions,
                )
            }
        }

        composeRule.onNodeWithTag("gps_gap").assertDoesNotExist()
        composeRule.onNodeWithTag("confirm_finish").assertIsNotEnabled()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    private fun showPermission(prompt: PermissionPrompt): () -> Int {
        var prompts = 0
        var starts = 0
        composeRule.setContent {
            ExpedientesTheme {
                HomeScreen(
                    state = HomeUiState(today = today, content = HomeContent.Idle("John", null)),
                    actions = actions,
                    shiftActions = shiftActions,
                    now = Instant.EPOCH,
                    onOpenProfile = {},
                    onStartClick = { starts++ },
                    permissionPrompt = prompt,
                    onPermissionAction = { prompts++ },
                )
            }
        }
        composeRule.onNodeWithTag("start_shift").performClick()
        assertEquals(1, starts)
        return { prompts }
    }

    @Test
    fun deniedPermissionAsksAgain() {
        val count = showPermission(PermissionPrompt.Ask)

        composeRule
            .onNodeWithText("Permita a localização para registrar o trajeto e calcular km e combustível.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Permitir").performClick()

        assertEquals(1, count())
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun permanentlyDeniedPermissionOpensSettings() {
        val count = showPermission(PermissionPrompt.OpenSettings)

        composeRule.onNodeWithText("Abrir configurações").performClick()

        assertEquals(1, count())
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
