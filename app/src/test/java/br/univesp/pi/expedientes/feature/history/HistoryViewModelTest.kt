package br.univesp.pi.expedientes.feature.history

import br.univesp.pi.expedientes.MainDispatcherRule
import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.PeriodSummary
import br.univesp.pi.expedientes.feature.home.ShiftStartRequests
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val clock = Clock.fixed(Instant.parse("2026-10-07T02:00:00Z"), zone)
    private val today = LocalDate.of(2026, 10, 6)
    private val data =
        HistoryData(
            days = ApiJson.decodeFromString(ListSerializer(DailyTotals.serializer()), Fixtures.read("analytics_daily")),
            summary = ApiJson.decodeFromString(PeriodSummary.serializer(), Fixtures.read("analytics_summary")),
            shiftInProgress = false,
        )

    private class FakeHistoryRepository : HistoryRepository {
        val results = mutableListOf<ApiResult<HistoryData>>()
        val calls = mutableListOf<Triple<LocalDate, LocalDate, ZoneId>>()
        private var last: ApiResult<HistoryData>? = null

        override suspend fun load(
            from: LocalDate,
            to: LocalDate,
            zone: ZoneId,
        ): ApiResult<HistoryData> {
            calls += Triple(from, to, zone)
            val next = results.removeFirstOrNull() ?: last ?: error("sem resposta")
            last = next
            return next
        }
    }

    private val repository = FakeHistoryRepository()
    private val requests = ShiftStartRequests()

    private fun TestScope.viewModel(): HistoryViewModel {
        val viewModel = HistoryViewModel(repository, clock, requests)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun loadsLastThirtyDaysInLocalZone() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.results += ApiResult.Success(data)

            val state = viewModel().state.value

            assertEquals(listOf(Triple(LocalDate.of(2026, 9, 7), today, zone)), repository.calls)
            assertEquals(today, state.today)
            assertEquals(HistoryContent.Loaded(data), state.content)
        }

    @Test
    fun periodChangeReloads() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.results += ApiResult.Success(data)
            repository.results += ApiResult.Success(data)
            val viewModel = viewModel()
            viewModel.toggleDay(today)

            viewModel.selectPeriod(7)
            assertEquals(HistoryContent.Loading, viewModel.state.value.content)
            advanceUntilIdle()
            viewModel.selectPeriod(7)
            viewModel.selectPeriod(15)
            advanceUntilIdle()

            assertEquals(LocalDate.of(2026, 9, 30), repository.calls.last().first)
            assertEquals(2, repository.calls.size)
            assertEquals(7, viewModel.state.value.periodDays)
            assertTrue(
                viewModel.state.value.expanded
                    .isEmpty(),
            )
        }

    @Test
    fun failureThenRetry() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.results += ApiResult.NetworkError
            repository.results += ApiResult.Success(data)
            val viewModel = viewModel()
            assertEquals(HistoryContent.Failed(RequestFailure.Network), viewModel.state.value.content)

            viewModel.retry()
            advanceUntilIdle()

            assertEquals(HistoryContent.Loaded(data), viewModel.state.value.content)
        }

    @Test
    fun pullToRefreshFailureKeepsList() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.results += ApiResult.Success(data)
            repository.results += ApiResult.NetworkError
            val viewModel = viewModel()

            viewModel.pullToRefresh()
            assertEquals(true, viewModel.state.value.refreshing)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(HistoryContent.Loaded(data), state.content)
            assertEquals(false, state.refreshing)
            assertEquals(RequestFailure.Network, state.refreshFailure)
        }

    @Test
    fun reloadOnlyWhenAlreadyLoaded() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.results += ApiResult.NetworkError
            val viewModel = viewModel()

            viewModel.reload()
            advanceUntilIdle()
            assertEquals(1, repository.calls.size)

            repository.results += ApiResult.Success(data)
            viewModel.retry()
            advanceUntilIdle()
            viewModel.reload()
            advanceUntilIdle()
            assertEquals(3, repository.calls.size)
        }

    @Test
    fun toggleDayExpandsAndCollapses() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.results += ApiResult.Success(data)
            val viewModel = viewModel()

            viewModel.toggleDay(today)
            assertEquals(setOf(today), viewModel.state.value.expanded)
            viewModel.toggleDay(today)
            assertTrue(
                viewModel.state.value.expanded
                    .isEmpty(),
            )
        }

    @Test
    fun startShiftAsksHome() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.results += ApiResult.Success(data)

            viewModel().startShift()

            assertEquals(true, requests.pending.value)
        }
}
