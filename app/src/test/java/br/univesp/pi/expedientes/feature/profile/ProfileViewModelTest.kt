package br.univesp.pi.expedientes.feature.profile

import br.univesp.pi.expedientes.MainDispatcherRule
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.FuelPriceResponse
import br.univesp.pi.expedientes.core.network.api.UserResponse
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import br.univesp.pi.expedientes.core.session.FakeSessionStore
import br.univesp.pi.expedientes.core.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-10-06T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val user = UserResponse("u1", "John Doe", "john@example.com", now)
    private val honda =
        VehicleResponse("v1", "Honda CG 160", FuelIds.GASOLINE, "Gasolina", BigDecimal("32.00"), 160, 2022, now)

    private inner class FakeProfileRepository : ProfileRepository {
        var loadResult: ApiResult<ProfileData> =
            ApiResult.Success(
                ProfileData(
                    user,
                    null,
                    listOf(FuelPriceResponse(FuelIds.GASOLINE, "Gasolina", BigDecimal("6.200"), now)),
                ),
            )
        var vehicleResult: ((String?, VehicleInput) -> ApiResult<VehicleResponse>) = { id, input ->
            ApiResult.Success(honda.copy(id = id ?: "novo", model = input.model, declaredKml = input.declaredKml))
        }
        val names = mutableListOf<String>()
        val savedVehicles = mutableListOf<Pair<String?, VehicleInput>>()
        val savedPrices = mutableListOf<Map<String, BigDecimal>>()
        var loads = 0

        override suspend fun load(): ApiResult<ProfileData> {
            loads++
            return loadResult
        }

        override suspend fun updateName(name: String): ApiResult<UserResponse> {
            names += name
            return ApiResult.Success(user.copy(name = name))
        }

        override suspend fun saveVehicle(
            vehicleId: String?,
            input: VehicleInput,
        ): ApiResult<VehicleResponse> {
            savedVehicles += vehicleId to input
            return vehicleResult(vehicleId, input)
        }

        override suspend fun updatePrices(prices: Map<String, BigDecimal>): ApiResult<List<FuelPriceResponse>> {
            savedPrices += prices
            return ApiResult.Success(
                prices.map { (id, price) ->
                    FuelPriceResponse(
                        id,
                        if (id ==
                            FuelIds.ETHANOL
                        ) {
                            "Etanol"
                        } else {
                            "Gasolina"
                        },
                        price,
                        now,
                    )
                },
            )
        }
    }

    private val repository = FakeProfileRepository()
    private val store = FakeSessionStore("token")

    private lateinit var sessionManager: SessionManager

    private fun TestScope.viewModel(): ProfileViewModel {
        sessionManager = SessionManager(store, backgroundScope)
        val viewModel = ProfileViewModel(repository, sessionManager, clock)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun loadsProfile() =
        runTest(mainDispatcherRule.dispatcher) {
            val state = viewModel().state.value

            assertEquals(false, state.isLoading)
            assertEquals("John Doe", state.name)
            assertNull(state.vehicle)
            assertEquals(mapOf(FuelIds.GASOLINE to BigDecimal("6.200")), state.prices)
        }

    @Test
    fun loadFailureCanBeRetried() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loadResult = ApiResult.NetworkError
            val viewModel = viewModel()
            assertEquals(RequestFailure.Network, viewModel.state.value.loadFailure)

            repository.loadResult = ApiResult.Success(ProfileData(user, honda, emptyList()))
            viewModel.retry()
            advanceUntilIdle()

            assertNull(viewModel.state.value.loadFailure)
            assertEquals(honda, viewModel.state.value.vehicle)
            assertEquals(2, repository.loads)
        }

    @Test
    fun savesNameOnlyWhenChanged() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.saveName()
            viewModel.onNameChange("  John Silva ")
            viewModel.saveName()
            advanceUntilIdle()

            assertEquals(listOf("John Silva"), repository.names)
            assertEquals(NameStatus.Saved, viewModel.state.value.nameStatus)
            assertEquals("John Silva", viewModel.state.value.savedName)
        }

    @Test
    fun blankNameIsNotSaved() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.onNameChange("  ")
            viewModel.saveName()

            assertEquals(NameStatus.Invalid(ProfileError.BlankName), viewModel.state.value.nameStatus)
            assertTrue(repository.names.isEmpty())
        }

    @Test
    fun createsVehicle() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            viewModel.openVehicleSheet()
            assertEquals(VehicleFormState(), viewModel.state.value.vehicleSheet)

            viewModel.onVehicleChange(VehicleField.Model, "Fan 160")
            viewModel.onVehicleFuelChange(FuelIds.FLEX)
            viewModel.onVehicleChange(VehicleField.Kml, "35,5")
            viewModel.saveVehicle()
            advanceUntilIdle()

            assertEquals(
                listOf(null to VehicleInput("Fan 160", FuelIds.FLEX, BigDecimal("35.5"), null, null)),
                repository.savedVehicles,
            )
            assertNull(viewModel.state.value.vehicleSheet)
            assertEquals(
                "Fan 160",
                viewModel.state.value.vehicle
                    ?.model,
            )
        }

    @Test
    fun updateOpensPrefilledAndSendsId() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loadResult = ApiResult.Success(ProfileData(user, honda, emptyList()))
            val viewModel = viewModel()

            viewModel.openVehicleSheet()
            val form = viewModel.state.value.vehicleSheet!!
            assertEquals("v1", form.vehicleId)
            assertEquals("32", form.kml)
            assertEquals("160", form.displacement)
            assertEquals("2022", form.year)

            viewModel.onVehicleChange(VehicleField.Year, "")
            viewModel.saveVehicle()
            advanceUntilIdle()

            assertEquals("v1", repository.savedVehicles.single().first)
            assertNull(
                repository.savedVehicles
                    .single()
                    .second.year,
            )
        }

    @Test
    fun invalidVehicleIsNotSent() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            viewModel.openVehicleSheet()

            viewModel.saveVehicle()

            assertEquals(
                mapOf(VehicleField.Model to ProfileError.BlankModel, VehicleField.Kml to ProfileError.InvalidKml),
                viewModel.state.value.vehicleSheet
                    ?.errors,
            )
            assertTrue(repository.savedVehicles.isEmpty())
        }

    @Test
    fun apiErrorStaysInsideSheet() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.vehicleResult = { _, _ -> ApiResult.HttpError(422, "Combustível não encontrado") }
            val viewModel = viewModel()
            viewModel.openVehicleSheet()
            viewModel.onVehicleChange(VehicleField.Model, "Fan")
            viewModel.onVehicleChange(VehicleField.Kml, "35")

            viewModel.saveVehicle()
            advanceUntilIdle()

            val form = viewModel.state.value.vehicleSheet!!
            assertEquals(RequestFailure.Api(422, "Combustível não encontrado"), form.failure)
            assertEquals(false, form.isSaving)
        }

    @Test
    fun savesPrices() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            viewModel.openPricesSheet()
            assertEquals(
                "6,20",
                viewModel.state.value.pricesSheet
                    ?.values
                    ?.get(FuelIds.GASOLINE),
            )

            viewModel.onPriceChange(FuelIds.ETHANOL, "3,75")
            viewModel.savePrices()
            advanceUntilIdle()

            assertEquals(
                listOf(mapOf(FuelIds.GASOLINE to BigDecimal("6.20"), FuelIds.ETHANOL to BigDecimal("3.75"))),
                repository.savedPrices,
            )
            assertNull(viewModel.state.value.pricesSheet)
            assertEquals(BigDecimal("3.75"), viewModel.state.value.prices[FuelIds.ETHANOL])
        }

    @Test
    fun logoutEndsSession() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            sessionManager.startSession("token")
            assertEquals("token", sessionManager.currentToken)

            viewModel.logout()

            assertNull(sessionManager.currentToken)
        }
}
