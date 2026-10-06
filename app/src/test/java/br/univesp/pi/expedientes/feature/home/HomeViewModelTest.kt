package br.univesp.pi.expedientes.feature.home

import br.univesp.pi.expedientes.MainDispatcherRule
import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.EarningResponse
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import br.univesp.pi.expedientes.feature.tracking.TrackingStatus
import br.univesp.pi.expedientes.feature.tracking.data.FlushResult
import br.univesp.pi.expedientes.testing.FakeTrackingController
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

        var current: ApiResult<ShiftResponse?> = ApiResult.Success(null)
        var last: ApiResult<EarningResponse?> = ApiResult.Success(null)
        var earningResult: ApiResult<EarningResponse>? = null
        var finish: ApiResult<ShiftResponse>? = null
        val calls = mutableListOf<String>()

        override suspend fun currentShift(): ApiResult<ShiftResponse?> {
            calls += "current"
            return current
        }

        override suspend fun lastEarning(shiftId: String) = last

        override suspend fun addEarning(
            shiftId: String,
            amount: BigDecimal,
        ): ApiResult<EarningResponse> {
            calls += "add:$shiftId:$amount"
            return earningResult!!
        }

        override suspend fun overwriteEarning(
            shiftId: String,
            earningId: String,
            amount: BigDecimal,
        ): ApiResult<EarningResponse> {
            calls += "overwrite:$earningId:$amount"
            return earningResult!!
        }

        override suspend fun finishShift(shiftId: String): ApiResult<ShiftResponse> {
            calls += "finish:$shiftId"
            return finish!!
        }
    }

    private val repository = FakeHomeRepository()
    private val tracking = FakeTrackingController()
    private val trackingStatus = TrackingStatus()
    private val idle get() = ApiResult.Success(HomeData("John Doe", null, lastDay))

    private fun TestScope.viewModel(): HomeViewModel {
        val viewModel = HomeViewModel(repository, clock, tracking, trackingStatus)
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

    @Test
    fun startedShiftStartsTracking() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.vehicle = ApiResult.Success(vehicle(FuelIds.GASOLINE))
            repository.start = ApiResult.Success(shift)
            val viewModel = viewModel()
            assertTrue(tracking.calls.isEmpty())

            viewModel.startShift()
            advanceUntilIdle()

            assertEquals(listOf("start:${shift.id}"), tracking.calls)
        }

    @Test
    fun failedStartDoesNotTrack() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            repository.vehicle = ApiResult.Success(vehicle(FuelIds.GASOLINE))
            repository.start = ApiResult.NetworkError
            val viewModel = viewModel()

            viewModel.startShift()
            advanceUntilIdle()

            assertTrue(tracking.calls.isEmpty())
        }

    @Test
    fun openingWithShiftInProgressResumesTracking() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += ApiResult.Success(HomeData("John Doe", shift, lastDay))

            viewModel()

            assertEquals(listOf("ensure:${shift.id}"), tracking.calls)
        }

    @Test
    fun gpsIndicatorFollowsTrackingStatus() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.loads += idle
            val viewModel = viewModel()
            assertEquals(false, viewModel.state.value.gpsActive)

            trackingStatus.running(true)
            advanceUntilIdle()

            assertEquals(true, viewModel.state.value.gpsActive)
        }

    private val inProgress get() = ApiResult.Success(HomeData("John Doe", shift, lastDay))
    private val earning = ApiJson.decodeFromString(EarningResponse.serializer(), Fixtures.read("earning"))

    private fun TestScope.runningShift(): HomeViewModel {
        repository.loads += inProgress
        val viewModel = viewModel()
        tracking.calls.clear()
        return viewModel
    }

    @Test
    fun refreshInProgressOnlyReloadsTheShift() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            val updated = shift.copy(summary = shift.summary.copy(profit = BigDecimal("99.00")))
            repository.current = ApiResult.Success(updated)

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(listOf("current"), repository.calls)
            assertEquals(HomeContent.InProgress("John Doe", updated), viewModel.state.value.content)
        }

    @Test
    fun refreshKeepsShiftWhenOffline() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            repository.current = ApiResult.NetworkError

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(HomeContent.InProgress("John Doe", shift), viewModel.state.value.content)
        }

    @Test
    fun shiftFinishedElsewhereStopsTracking() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            trackingStatus.running(true)
            repository.current = ApiResult.Success(null)
            repository.loads += idle

            viewModel.refresh()
            advanceUntilIdle()

            assertTrue(viewModel.state.value.content is HomeContent.Idle)
            assertEquals(listOf("stop"), tracking.calls)
        }

    @Test
    fun earningSheetLoadsLastEarningAndPresetFillsAmount() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            repository.last = ApiResult.Success(earning)

            viewModel.openEarning()
            advanceUntilIdle()
            viewModel.choosePreset(BigDecimal("12"))

            val flow = viewModel.state.value.earning!!
            assertEquals(earning, flow.lastEarning)
            assertEquals("12,00", flow.text)
            assertEquals(BigDecimal("12.00"), flow.amount)
        }

    @Test
    fun invalidAmountsAreNotSent() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            viewModel.openEarning()

            listOf("", "0", "-5", "abc", "1,234").forEach {
                viewModel.changeEarning(it)
                assertEquals(
                    null,
                    viewModel.state.value.earning!!
                        .amount,
                )
                viewModel.addEarning()
            }
            advanceUntilIdle()

            assertTrue(repository.calls.isEmpty())
        }

    @Test
    fun addEarningClosesSheetAndRefreshes() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            repository.earningResult = ApiResult.Success(earning)
            repository.current = ApiResult.Success(shift)
            viewModel.openEarning()
            viewModel.changeEarning("7,50")

            viewModel.addEarning()
            viewModel.addEarning()
            advanceUntilIdle()

            assertEquals(listOf("add:${shift.id}:7.50", "current"), repository.calls)
            assertEquals(null, viewModel.state.value.earning)
        }

    @Test
    fun overwriteUsesLastEarning() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            repository.last = ApiResult.Success(earning)
            repository.earningResult = ApiResult.Success(earning)
            viewModel.openEarning()
            advanceUntilIdle()
            viewModel.choosePreset(BigDecimal("5"))

            viewModel.overwriteEarning()
            advanceUntilIdle()

            assertEquals("overwrite:${earning.id}:5.00", repository.calls.first())
        }

    @Test
    fun overwriteWithoutLastEarningDoesNothing() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            viewModel.openEarning()
            viewModel.choosePreset(BigDecimal("5"))

            viewModel.overwriteEarning()
            advanceUntilIdle()

            assertTrue(repository.calls.isEmpty())
            assertEquals(
                false,
                viewModel.state.value.earning!!
                    .saving,
            )
        }

    @Test
    fun earningFailureKeepsSheetWithMessage() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            repository.earningResult = ApiResult.HttpError(409, "Expediente já encerrado")
            viewModel.openEarning()
            viewModel.choosePreset(BigDecimal("8"))

            viewModel.addEarning()
            advanceUntilIdle()

            val flow = viewModel.state.value.earning!!
            assertEquals(RequestFailure.Api(409, "Expediente já encerrado"), flow.failure)
            assertEquals(false, flow.saving)
        }

    @Test
    fun finishStopsGpsSendsQueueThenFinishes() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            repository.finish = ApiResult.Success(shift)
            repository.loads += idle

            viewModel.openFinish()
            viewModel.confirmFinish()
            viewModel.confirmFinish()
            advanceUntilIdle()

            assertEquals(listOf("stop", "flush"), tracking.calls)
            assertEquals(listOf("finish:${shift.id}"), repository.calls)
            assertEquals(null, viewModel.state.value.finish)
            assertTrue(viewModel.state.value.content is HomeContent.Idle)
        }

    @Test
    fun finishOfflineKeepsShiftAndRestartsGps() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            tracking.flushResult = FlushResult.Retry

            viewModel.openFinish()
            viewModel.confirmFinish()
            advanceUntilIdle()

            assertEquals(listOf("stop", "flush", "start:${shift.id}"), tracking.calls)
            assertTrue(repository.calls.isEmpty())
            assertEquals(FinishFlow(failure = RequestFailure.Network), viewModel.state.value.finish)
        }

    @Test
    fun finishErrorRestartsGps() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            repository.finish = ApiResult.HttpError(500, "falhou")

            viewModel.openFinish()
            viewModel.confirmFinish()
            advanceUntilIdle()

            assertEquals("start:${shift.id}", tracking.calls.last())
            assertEquals(
                RequestFailure.Api(500, "falhou"),
                viewModel.state.value.finish
                    ?.failure,
            )
        }

    @Test
    fun finishAlreadyDoneElsewhereIsTreatedAsSuccess() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()
            repository.finish = ApiResult.HttpError(409, "Expediente já encerrado")
            repository.loads += idle

            viewModel.openFinish()
            viewModel.confirmFinish()
            advanceUntilIdle()

            assertEquals(null, viewModel.state.value.finish)
            assertTrue(viewModel.state.value.content is HomeContent.Idle)
        }

    @Test
    fun continueClosesFinishSheet() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()

            viewModel.openFinish()
            viewModel.dismissFinish()

            assertEquals(null, viewModel.state.value.finish)
            assertTrue(tracking.calls.isEmpty())
        }

    @Test
    fun resumeTrackingRestartsService() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()

            viewModel.resumeTracking()
            advanceUntilIdle()

            assertEquals(listOf("start:${shift.id}"), tracking.calls)
        }

    @Test
    fun lastFixFollowsTrackingStatus() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = runningShift()

            trackingStatus.fixReceived(Instant.EPOCH)
            advanceUntilIdle()

            assertEquals(Instant.EPOCH, viewModel.state.value.lastFixAt)
        }
}
