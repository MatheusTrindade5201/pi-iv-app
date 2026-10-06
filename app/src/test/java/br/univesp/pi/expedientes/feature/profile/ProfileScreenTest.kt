package br.univesp.pi.expedientes.feature.profile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class RecordingActions : ProfileActions {
        val calls = mutableListOf<String>()

        override fun retry() {
            calls += "retry"
        }

        override fun onNameChange(value: String) {}

        override fun saveName() {}

        override fun openVehicleSheet() {
            calls += "openVehicle"
        }

        override fun onVehicleChange(
            field: VehicleField,
            value: String,
        ) {}

        override fun onVehicleFuelChange(fuelId: String) {
            calls += "fuel:$fuelId"
        }

        override fun saveVehicle() {
            calls += "saveVehicle"
        }

        override fun closeVehicleSheet() {}

        override fun setMeasureHelpVisible(visible: Boolean) {
            calls += "help:$visible"
        }

        override fun openPricesSheet() {
            calls += "openPrices"
        }

        override fun onPriceChange(
            fuelId: String,
            value: String,
        ) {}

        override fun savePrices() {}

        override fun closePricesSheet() {}

        override fun logout() {
            calls += "logout"
        }
    }

    private val actions = RecordingActions()
    private val loaded =
        ProfileUiState(
            isLoading = false,
            name = "John Doe",
            savedName = "John Doe",
            prices = mapOf(FuelIds.GASOLINE to BigDecimal("6.200"), FuelIds.ETHANOL to BigDecimal("3.750")),
        )

    private fun show(state: ProfileUiState) {
        composeRule.setContent { ExpedientesTheme { ProfileScreen(state, actions) } }
    }

    @Test
    fun withoutVehicleShowsAddCard() {
        show(loaded)

        composeRule.onNodeWithText("Perfil").assertIsDisplayed()
        composeRule.onNodeWithText("John Doe").assertIsDisplayed()
        composeRule.onNodeWithText("Cadastrar veículo").performClick()
        composeRule.onNodeWithText("R$ 6,20").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 3,75").assertIsDisplayed()

        assertEquals(listOf("openVehicle"), actions.calls)
    }

    @Test
    fun withVehicleShowsDetailsAndUpdate() {
        val vehicle =
            VehicleResponse(
                "v1",
                "Honda",
                FuelIds.GASOLINE,
                "Gasolina",
                BigDecimal("32.00"),
                null,
                null,
                Instant.EPOCH,
            )
        show(loaded.copy(vehicle = vehicle))

        composeRule.onNodeWithText("Minha moto").assertIsDisplayed()
        composeRule.onNodeWithText("Honda").assertIsDisplayed()
        composeRule.onAllNodesWithText("Gasolina").assertCountEquals(2)
        composeRule.onNodeWithText("32 km/l").assertIsDisplayed()
        composeRule.onNodeWithText("Atualizar").performClick()
        composeRule.onNodeWithText("Cadastrar veículo").assertDoesNotExist()

        assertEquals(listOf("openVehicle"), actions.calls)
    }

    @Test
    fun editPricesAndLogout() {
        show(loaded)

        composeRule.onNodeWithTag("edit_prices").performClick()
        composeRule.onNodeWithTag("logout").performScrollTo().performClick()

        assertEquals(listOf("openPrices", "logout"), actions.calls)
    }

    @Test
    fun missingPriceShowsDash() {
        show(loaded.copy(prices = mapOf(FuelIds.GASOLINE to BigDecimal("6.2"))))

        composeRule.onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun loadFailureOffersRetry() {
        show(ProfileUiState(isLoading = false, loadFailure = RequestFailure.Network))

        composeRule.onNodeWithText("Tentar de novo").performClick()

        assertEquals(listOf("retry"), actions.calls)
    }

    @Test
    fun vehicleFormMatchesPrototype() {
        val calls = mutableListOf<String>()
        composeRule.setContent {
            ExpedientesTheme {
                VehicleForm(
                    state =
                        VehicleFormState(
                            fuelId = FuelIds.FLEX,
                            errors =
                                mapOf(
                                    VehicleField.Kml to ProfileError.InvalidKml,
                                ),
                        ),
                    onChange = { _, _ -> },
                    onFuelChange = { calls += it },
                    onMeasureHelp = { calls += "help" },
                    onSave = { calls += "save" },
                )
            }
        }

        listOf("Cadastrar moto", "Modelo", "Combustível", "Consumo médio", "km/l", "Cilindrada", "Ano").forEach {
            composeRule.onNodeWithText(it).assertIsDisplayed()
        }
        composeRule.onNodeWithTag("chip_${FuelIds.FLEX}").assertIsSelected()
        composeRule.onNodeWithText("Informe um consumo entre 0 e 999,99 km/l").assertIsDisplayed()

        composeRule.onNodeWithText("Etanol").performClick()
        composeRule.onNodeWithText("Como medir?").performClick()
        composeRule.onNodeWithTag("vehicle_save").performClick()

        assertEquals(listOf(FuelIds.ETHANOL, "help", "save"), calls)
    }

    @Test
    fun pricesFormShowsFuels() {
        composeRule.setContent {
            ExpedientesTheme {
                PricesForm(
                    state = PricesFormState(values = mapOf(FuelIds.GASOLINE to "6,20")),
                    onChange = { _, _ -> },
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("Valores do combustível").assertIsDisplayed()
        composeRule.onNodeWithText("Gasolina").assertIsDisplayed()
        composeRule.onNodeWithText("Etanol").assertIsDisplayed()
        composeRule.onNodeWithText("6,20").assertIsDisplayed()
    }
}
