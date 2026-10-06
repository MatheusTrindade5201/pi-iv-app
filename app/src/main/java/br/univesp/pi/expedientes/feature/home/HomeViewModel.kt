package br.univesp.pi.expedientes.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.network.failureOrNull
import br.univesp.pi.expedientes.feature.tracking.TrackingController
import br.univesp.pi.expedientes.feature.tracking.TrackingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

sealed interface HomeContent {
    data object Loading : HomeContent

    data class Failed(
        val failure: RequestFailure,
    ) : HomeContent

    data class Idle(
        val userName: String,
        val lastDay: DailyTotals?,
    ) : HomeContent

    data class InProgress(
        val userName: String,
        val shift: ShiftResponse,
    ) : HomeContent
}

sealed interface StartFlow {
    data object None : StartFlow

    data object Checking : StartFlow

    data object NoVehicle : StartFlow

    data class ChooseFuel(
        val vehicleId: String,
        val fuelId: String = FuelIds.GASOLINE,
        val isStarting: Boolean = false,
    ) : StartFlow

    data class Error(
        val failure: RequestFailure,
        val offerProfile: Boolean,
    ) : StartFlow
}

data class HomeUiState(
    val today: LocalDate,
    val content: HomeContent = HomeContent.Loading,
    val startFlow: StartFlow = StartFlow.None,
    val gpsActive: Boolean = false,
) {
    val isStarting: Boolean
        get() = startFlow == StartFlow.Checking || (startFlow as? StartFlow.ChooseFuel)?.isStarting == true
}

interface HomeActions {
    fun retry()

    fun startShift()

    fun chooseFuel(fuelId: String)

    fun confirmFuel()

    fun dismissStartFlow()
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val repository: HomeRepository,
        private val clock: Clock,
        private val tracking: TrackingController,
        trackingStatus: TrackingStatus,
    ) : ViewModel(),
        HomeActions {
        private val _state = MutableStateFlow(HomeUiState(today = LocalDate.now(clock)))
        val state: StateFlow<HomeUiState> = _state.asStateFlow()
        private var loading = false

        init {
            load()
            viewModelScope.launch {
                trackingStatus.isRunning.collect { running -> _state.update { it.copy(gpsActive = running) } }
            }
        }

        override fun retry() {
            _state.update { it.copy(content = HomeContent.Loading) }
            load()
        }

        fun refresh() {
            if (_state.value.content !is HomeContent.Loading) load()
        }

        private fun load(afterLoad: (HomeContent) -> Unit = {}) {
            if (loading) return
            loading = true
            viewModelScope.launch {
                val content =
                    when (val result = repository.load()) {
                        is ApiResult.Success -> result.value.toContent()
                        else ->
                            _state.value.content.takeUnless { it is HomeContent.Loading || it is HomeContent.Failed }
                                ?: HomeContent.Failed(requireNotNull(result.failureOrNull()))
                    }
                _state.update { it.copy(today = LocalDate.now(clock), content = content) }
                loading = false
                if (content is HomeContent.InProgress) tracking.ensureRunning(content.shift.id)
                afterLoad(content)
            }
        }

        override fun startShift() {
            if (_state.value.isStarting) return
            setFlow(StartFlow.Checking)
            viewModelScope.launch {
                when (val result = repository.firstVehicle()) {
                    is ApiResult.Success -> {
                        val vehicle = result.value
                        when {
                            vehicle == null -> setFlow(StartFlow.NoVehicle)
                            vehicle.fuelId == FuelIds.FLEX -> setFlow(StartFlow.ChooseFuel(vehicle.id))
                            else -> send(vehicle.id, null)
                        }
                    }
                    else -> setFlow(StartFlow.Error(requireNotNull(result.failureOrNull()), offerProfile = false))
                }
            }
        }

        override fun chooseFuel(fuelId: String) {
            val flow = _state.value.startFlow as? StartFlow.ChooseFuel ?: return
            if (!flow.isStarting) setFlow(flow.copy(fuelId = fuelId))
        }

        override fun confirmFuel() {
            val flow = _state.value.startFlow as? StartFlow.ChooseFuel ?: return
            if (flow.isStarting) return
            setFlow(flow.copy(isStarting = true))
            viewModelScope.launch { send(flow.vehicleId, flow.fuelId) }
        }

        override fun dismissStartFlow() {
            if (!_state.value.isStarting) setFlow(StartFlow.None)
        }

        private suspend fun send(
            vehicleId: String,
            fuelId: String?,
        ) {
            val result = repository.startShift(vehicleId, fuelId)
            when {
                result is ApiResult.Success -> {
                    _state.update {
                        it.copy(content = HomeContent.InProgress(userName(), result.value), startFlow = StartFlow.None)
                    }
                    tracking.start(result.value.id)
                }
                result is ApiResult.HttpError && result.code == 409 -> {
                    val failure = RequestFailure.Api(result.code, result.message)
                    load { content ->
                        setFlow(
                            if (content is HomeContent.InProgress) {
                                StartFlow.None
                            } else {
                                StartFlow.Error(failure, offerProfile = true)
                            },
                        )
                    }
                }
                else -> setFlow(StartFlow.Error(requireNotNull(result.failureOrNull()), offerProfile = false))
            }
        }

        private fun setFlow(flow: StartFlow) {
            _state.update { it.copy(startFlow = flow) }
        }

        private fun userName(): String =
            when (val content = _state.value.content) {
                is HomeContent.Idle -> content.userName
                is HomeContent.InProgress -> content.userName
                else -> ""
            }

        private fun HomeData.toContent(): HomeContent =
            if (currentShift != null) {
                HomeContent.InProgress(userName, currentShift)
            } else {
                HomeContent.Idle(userName, lastDay)
            }
    }
