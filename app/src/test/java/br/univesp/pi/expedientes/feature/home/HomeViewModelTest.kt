package br.univesp.pi.expedientes.feature.home

import br.univesp.pi.expedientes.MainDispatcherRule
import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-06T15:00:00Z"), ZoneOffset.UTC)
    private val shift = ApiJson.decodeFromString(ShiftResponse.serializer(), Fixtures.read("shift_current"))
    private val lastDay =
        ApiJson.decodeFromString(ListSerializer(DailyTotals.serializer()), Fixtures.read("analytics_daily")).first()

    private fun vehicle(fuelId: String) =
        VehicleResponse("v1", "Honda", fuelId, "x", BigDecimal("32"), null, null, Instant.EPOCH)

    private class FakeHomeRepository : HomeRepository {
        val loads = mutableListOf<ApiResult<HomeData>>()
        var lastLoad: ApiResult<HomeData>? = null
        var vehicle: ApiResult<VehicleResponse?> = ApiResult.Success(null)
        var start: ApiResult<ShiftResponse>? = null
        var startGate: CompletableDeferred<Unit>? = null
        val starts = mutableListOf<Pair<String, String?>>()

        override suspend fun load(): ApiResult<HomeData> {
            val next = loads.removeFirstOrNull() ?: lastLoad!!
            lastLoad = next
            return next
        }

        override suspend fun firstVehicle() = vehicle

        override suspend fun startShift(
            vehicleId: String,
            fuelId: String?,
        ): ApiResult<ShiftResponse> {
            starts += vehicleId to fuelId
            startGate?.await()
            return start!!
        }
    }

    private val repository = FakeHomeRepository()
    private val idle get() = ApiResult.Success(HomeData("John Doe", null, lastDay))

    private fun TestScope.viewModel(): HomeViewModel {
        val viewModel = HomeViewModel(repository, clock)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun idleShowsLastDayAndToday() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle

            val state = viewModel().state.value

            assertEquals(LocalDate.of(2026, 10, 6), state.today)
            assertEquals(HomeContent.Idle("John Doe", lastDay), state.content)
        }

    @Test
    fun shiftInProgressIsShown() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += ApiResult.Success(HomeData("John Doe", shift, lastDay))

            assertEquals(HomeContent.InProgress("John Doe", shift), viewModel().state.value.content)
        }

    @Test
    fun loadFailureThenRetry() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += ApiResult.NetworkError
            repository.loads += idle
            val viewModel = viewModel()
            assertEquals(HomeContent.Failed(RequestFailure.Network), viewModel.state.value.content)

            viewModel.retry()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is HomeContent.Idle)
        }

    @Test
    fun refreshFailureKeepsCurrentContent() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.loads += ApiResult.NetworkError
            val viewModel = viewModel()

            viewModel.refresh()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is HomeContent.Idle)
        }

    @Test
    fun withoutVehicleAsksToRegisterIt() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            val viewModel = viewModel()

            viewModel.startShift()
            advanceUntilIdle()

            assertEquals(StartFlow.NoVehicle, viewModel.state.value.startFlow)
            assertTrue(repository.starts.isEmpty())
        }

    @Test
    fun gasolineVehicleStartsDirectly() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.vehicle = ApiResult.Success(vehicle(FuelIds.GASOLINE))
            repository.start = ApiResult.Success(shift)
            val viewModel = viewModel()

            viewModel.startShift()
            advanceUntilIdle()

            assertEquals(listOf("v1" to null), repository.starts)
            assertEquals(HomeContent.InProgress("John Doe", shift), viewModel.state.value.content)
            assertEquals(StartFlow.None, viewModel.state.value.startFlow)
        }

    @Test
    fun flexVehicleAsksForFuel() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.vehicle = ApiResult.Success(vehicle(FuelIds.FLEX))
            repository.start = ApiResult.Success(shift)
            val viewModel = viewModel()

            viewModel.startShift()
            advanceUntilIdle()
            assertEquals(StartFlow.ChooseFuel("v1"), viewModel.state.value.startFlow)

            viewModel.chooseFuel(FuelIds.ETHANOL)
            viewModel.confirmFuel()
            advanceUntilIdle()

            assertEquals(listOf("v1" to FuelIds.ETHANOL), repository.starts)
            assertTrue(viewModel.state.value.content is HomeContent.InProgress)
        }

    @Test
    fun conflictWithShiftAlreadyRunningShowsIt() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.loads += ApiResult.Success(HomeData("John Doe", shift, lastDay))
            repository.vehicle = ApiResult.Success(vehicle(FuelIds.GASOLINE))
            repository.start = ApiResult.HttpError(409, "Já existe um expediente em andamento")
            val viewModel = viewModel()

            viewModel.startShift()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is HomeContent.InProgress)
            assertEquals(StartFlow.None, viewModel.state.value.startFlow)
        }

    @Test
    fun conflictWithoutShiftOffersProfile() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.loads += idle
            repository.vehicle = ApiResult.Success(vehicle(FuelIds.GASOLINE))
            repository.start = ApiResult.HttpError(409, "Cadastre o preço do combustível antes de iniciar")
            val viewModel = viewModel()

            viewModel.startShift()
            advanceUntilIdle()

            assertEquals(
                StartFlow.Error(RequestFailure.Api(409, "Cadastre o preço do combustível antes de iniciar"), true),
                viewModel.state.value.startFlow,
            )
        }

    @Test
    fun networkErrorDoesNotOfferProfile() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.vehicle = ApiResult.Success(vehicle(FuelIds.GASOLINE))
            repository.start = ApiResult.NetworkError
            val viewModel = viewModel()

            viewModel.startShift()
            advanceUntilIdle()

            assertEquals(StartFlow.Error(RequestFailure.Network, false), viewModel.state.value.startFlow)
        }

    @Test
    fun doubleTapStartsOnce() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.vehicle = ApiResult.Success(vehicle(FuelIds.GASOLINE))
            repository.start = ApiResult.Success(shift)
            repository.startGate = CompletableDeferred()
            val viewModel = viewModel()

            viewModel.startShift()
            viewModel.startShift()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.isStarting)
            repository.startGate?.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, repository.starts.size)
        }
}
