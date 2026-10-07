package br.univesp.pi.expedientes.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.univesp.pi.expedientes.core.format.formatMoneyInput
import br.univesp.pi.expedientes.core.format.parseDecimal
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.EarningResponse
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.network.failureOrNull
import br.univesp.pi.expedientes.feature.tracking.TrackingController
import br.univesp.pi.expedientes.feature.tracking.TrackingStatus
import br.univesp.pi.expedientes.feature.tracking.data.FlushResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
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

data class EarningFlow(
    val text: String = "",
    val lastEarning: EarningResponse? = null,
    val saving: Boolean = false,
    val failure: RequestFailure? = null,
) {
    val amount: BigDecimal?
        get() = parseDecimal(text)?.takeIf { it.signum() > 0 && it.scale() <= 2 }
}

data class FinishFlow(
    val finishing: Boolean = false,
    val failure: RequestFailure? = null,
)

data class HomeUiState(
    val today: LocalDate,
    val content: HomeContent = HomeContent.Loading,
    val startFlow: StartFlow = StartFlow.None,
    val gpsActive: Boolean = false,
    val lastFixAt: Instant? = null,
    val earning: EarningFlow? = null,
    val finish: FinishFlow? = null,
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

interface ShiftActions {
    fun openEarning()

    fun changeEarning(text: String)

    fun choosePreset(value: BigDecimal)

    fun addEarning()

    fun overwriteEarning()

    fun dismissEarning()

    fun openFinish()

    fun confirmFinish()

    fun dismissFinish()

    fun resumeTracking()
}

val EARNING_PRESETS: List<BigDecimal> = listOf("5", "8", "12", "20").map(::BigDecimal)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val repository: HomeRepository,
        private val clock: Clock,
        private val tracking: TrackingController,
        private val trackingStatus: TrackingStatus,
        private val startRequests: ShiftStartRequests,
    ) : ViewModel(),
        HomeActions,
        ShiftActions {
        private val _state = MutableStateFlow(HomeUiState(today = LocalDate.now(clock)))
        val state: StateFlow<HomeUiState> = _state.asStateFlow()
        private var loading = false
        val startRequested: StateFlow<Boolean> = startRequests.pending

        fun consumeStartRequest(): Boolean {
            val content = _state.value.content
            if (content is HomeContent.Loading) return false
            return startRequests.consume() && content is HomeContent.Idle
        }

        init {
            load()
            viewModelScope.launch {
                trackingStatus.isRunning.collect { running -> _state.update { it.copy(gpsActive = running) } }
            }
            viewModelScope.launch {
                trackingStatus.lastFixAt.collect { at -> _state.update { it.copy(lastFixAt = at) } }
            }
        }

        override fun retry() {
            _state.update { it.copy(content = HomeContent.Loading) }
            load()
        }

        fun refresh() {
            when (_state.value.content) {
                HomeContent.Loading -> Unit
                is HomeContent.InProgress -> refreshShift()
                else -> load()
            }
        }

        private fun refreshShift() {
            if (loading) return
            loading = true
            viewModelScope.launch {
                val result = repository.currentShift()
                loading = false
                if (result !is ApiResult.Success) return@launch
                val shift = result.value
                if (shift == null) {
                    load()
                } else {
                    _state.update { state ->
                        val current = state.content as? HomeContent.InProgress ?: return@update state
                        state.copy(content = current.copy(shift = shift))
                    }
                }
            }
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
                when {
                    content is HomeContent.InProgress -> tracking.ensureRunning(content.shift.id)
                    content is HomeContent.Idle && trackingStatus.isRunning.value -> tracking.stop()
                }
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

        override fun openEarning() {
            val shift = currentShift() ?: return
            _state.update { it.copy(earning = EarningFlow()) }
            viewModelScope.launch {
                val last = repository.lastEarning(shift.id)
                if (last is ApiResult.Success) updateEarning { it.copy(lastEarning = last.value) }
            }
        }

        override fun changeEarning(text: String) {
            updateEarning { if (it.saving) it else it.copy(text = text, failure = null) }
        }

        override fun choosePreset(value: BigDecimal) = changeEarning(formatMoneyInput(value))

        override fun addEarning() = saveEarning { shiftId, amount, _ -> repository.addEarning(shiftId, amount) }

        override fun overwriteEarning() =
            saveEarning { shiftId, amount, last ->
                last?.let { repository.overwriteEarning(shiftId, it.id, amount) }
            }

        override fun dismissEarning() {
            if (_state.value.earning?.saving != true) _state.update { it.copy(earning = null) }
        }

        private fun saveEarning(call: suspend (String, BigDecimal, EarningResponse?) -> ApiResult<EarningResponse>?) {
            val shift = currentShift() ?: return
            val flow = _state.value.earning ?: return
            val amount = flow.amount ?: return
            if (flow.saving) return
            updateEarning { it.copy(saving = true, failure = null) }
            viewModelScope.launch {
                when (val result = call(shift.id, amount, flow.lastEarning)) {
                    null -> updateEarning { it.copy(saving = false) }
                    is ApiResult.Success -> {
                        _state.update { it.copy(earning = null) }
                        refresh()
                    }
                    else -> updateEarning { it.copy(saving = false, failure = result.failureOrNull()) }
                }
            }
        }

        private fun updateEarning(change: (EarningFlow) -> EarningFlow) {
            _state.update { state -> state.earning?.let { state.copy(earning = change(it)) } ?: state }
        }

        override fun openFinish() {
            if (currentShift() != null) _state.update { it.copy(finish = FinishFlow()) }
        }

        override fun dismissFinish() {
            if (_state.value.finish?.finishing != true) _state.update { it.copy(finish = null) }
        }

        override fun confirmFinish() {
            val shift = currentShift() ?: return
            if (_state.value.finish?.finishing != false) return
            _state.update { it.copy(finish = FinishFlow(finishing = true)) }
            viewModelScope.launch {
                tracking.stop()
                if (tracking.flushNow() == FlushResult.Retry) {
                    failFinish(shift.id, RequestFailure.Network)
                    return@launch
                }
                val result = repository.finishShift(shift.id)
                val alreadyFinished = result is ApiResult.HttpError && result.code == 409
                if (result is ApiResult.Success || alreadyFinished) {
                    _state.update { it.copy(finish = null) }
                    load()
                } else {
                    failFinish(shift.id, requireNotNull(result.failureOrNull()))
                }
            }
        }

        private suspend fun failFinish(
            shiftId: String,
            failure: RequestFailure,
        ) {
            tracking.start(shiftId)
            _state.update { it.copy(finish = FinishFlow(failure = failure)) }
        }

        override fun resumeTracking() {
            val shift = currentShift() ?: return
            viewModelScope.launch { tracking.start(shift.id) }
        }

        private fun currentShift(): ShiftResponse? = (_state.value.content as? HomeContent.InProgress)?.shift

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
