package br.univesp.pi.expedientes.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.failureOrNull
import br.univesp.pi.expedientes.feature.home.ShiftStartRequests
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

val HISTORY_PERIODS = listOf(7, 30, 90)

sealed interface HistoryContent {
    data object Loading : HistoryContent

    data class Failed(
        val failure: RequestFailure,
    ) : HistoryContent

    data class Loaded(
        val data: HistoryData,
    ) : HistoryContent
}

data class HistoryUiState(
    val today: LocalDate,
    val periodDays: Int = 30,
    val content: HistoryContent = HistoryContent.Loading,
    val refreshing: Boolean = false,
    val refreshFailure: RequestFailure? = null,
    val expanded: Set<LocalDate> = emptySet(),
)

interface HistoryActions {
    fun selectPeriod(days: Int)

    fun pullToRefresh()

    fun retry()

    fun toggleDay(date: LocalDate)

    fun startShift()
}

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val repository: HistoryRepository,
        private val clock: Clock,
        private val startRequests: ShiftStartRequests,
    ) : ViewModel(),
        HistoryActions {
        private val _state = MutableStateFlow(HistoryUiState(today = LocalDate.now(clock)))
        val state: StateFlow<HistoryUiState> = _state.asStateFlow()
        private var job: Job? = null

        init {
            load()
        }

        override fun selectPeriod(days: Int) {
            if (days == _state.value.periodDays || days !in HISTORY_PERIODS) return
            _state.update { it.copy(periodDays = days, content = HistoryContent.Loading, expanded = emptySet()) }
            load()
        }

        override fun pullToRefresh() {
            if (_state.value.content !is HistoryContent.Loaded) return retry()
            _state.update { it.copy(refreshing = true) }
            load()
        }

        fun reload() {
            if (_state.value.content is HistoryContent.Loaded && job?.isActive != true) load()
        }

        override fun retry() {
            _state.update { it.copy(content = HistoryContent.Loading) }
            load()
        }

        override fun toggleDay(date: LocalDate) {
            _state.update { it.copy(expanded = if (date in it.expanded) it.expanded - date else it.expanded + date) }
        }

        override fun startShift() {
            startRequests.request()
        }

        private fun load() {
            job?.cancel()
            job =
                viewModelScope.launch {
                    val today = LocalDate.now(clock)
                    val days = _state.value.periodDays
                    val result = repository.load(today.minusDays(days - 1L), today, clock.zone)
                    _state.update { state ->
                        when (result) {
                            is ApiResult.Success ->
                                state.copy(
                                    today = today,
                                    content = HistoryContent.Loaded(result.value),
                                    refreshing = false,
                                    refreshFailure = null,
                                )
                            else -> {
                                val failure = requireNotNull(result.failureOrNull())
                                if (state.content is HistoryContent.Loaded) {
                                    state.copy(refreshing = false, refreshFailure = failure)
                                } else {
                                    state.copy(content = HistoryContent.Failed(failure), refreshing = false)
                                }
                            }
                        }
                    }
                }
        }
    }
