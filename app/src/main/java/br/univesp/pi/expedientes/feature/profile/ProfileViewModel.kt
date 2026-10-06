package br.univesp.pi.expedientes.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.api.FuelPriceResponse
import br.univesp.pi.expedientes.core.network.failureOrNull
import br.univesp.pi.expedientes.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

interface ProfileActions {
    fun retry()

    fun onNameChange(value: String)

    fun saveName()

    fun openVehicleSheet()

    fun onVehicleChange(
        field: VehicleField,
        value: String,
    )

    fun onVehicleFuelChange(fuelId: String)

    fun saveVehicle()

    fun closeVehicleSheet()

    fun setMeasureHelpVisible(visible: Boolean)

    fun openPricesSheet()

    fun onPriceChange(
        fuelId: String,
        value: String,
    )

    fun savePrices()

    fun closePricesSheet()

    fun logout()
}

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val repository: ProfileRepository,
        private val sessionManager: SessionManager,
        private val clock: Clock,
    ) : ViewModel(),
        ProfileActions {
        private val _state = MutableStateFlow(ProfileUiState())
        val state: StateFlow<ProfileUiState> = _state.asStateFlow()

        init {
            load()
        }

        override fun retry() = load()

        private fun load() {
            _state.update { it.copy(isLoading = true, loadFailure = null) }
            viewModelScope.launch {
                when (val result = repository.load()) {
                    is ApiResult.Success ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                name = result.value.user.name,
                                savedName = result.value.user.name,
                                vehicle = result.value.vehicle,
                                prices = result.value.prices.toPriceMap(),
                            )
                        }
                    else -> _state.update { it.copy(isLoading = false, loadFailure = result.failureOrNull()) }
                }
            }
        }

        override fun onNameChange(value: String) {
            _state.update { it.copy(name = value, nameStatus = NameStatus.Idle) }
        }

        override fun saveName() {
            val current = _state.value
            val name = current.name.trim()
            if (name == current.savedName || current.nameStatus == NameStatus.Saving) return
            ProfileValidation.validateName(name)?.let { error ->
                _state.update { it.copy(nameStatus = NameStatus.Invalid(error)) }
                return
            }
            _state.update { it.copy(nameStatus = NameStatus.Saving) }
            viewModelScope.launch {
                when (val result = repository.updateName(name)) {
                    is ApiResult.Success ->
                        _state.update {
                            it.copy(
                                name = result.value.name,
                                savedName = result.value.name,
                                nameStatus = NameStatus.Saved,
                            )
                        }
                    else ->
                        _state.update {
                            it.copy(nameStatus = NameStatus.Failed(requireNotNull(result.failureOrNull())))
                        }
                }
            }
        }

        override fun openVehicleSheet() {
            _state.update { it.copy(vehicleSheet = VehicleFormState.from(it.vehicle)) }
        }

        override fun onVehicleChange(
            field: VehicleField,
            value: String,
        ) {
            updateVehicleSheet { it.edit(field, value) }
        }

        override fun onVehicleFuelChange(fuelId: String) {
            updateVehicleSheet { it.copy(fuelId = fuelId, failure = null) }
        }

        override fun saveVehicle() {
            val form = _state.value.vehicleSheet ?: return
            if (form.isSaving) return
            when (val validated = ProfileValidation.validateVehicle(form, LocalDate.now(clock).year)) {
                is Validated.Invalid -> updateVehicleSheet { it.copy(errors = validated.errors) }
                is Validated.Valid -> {
                    updateVehicleSheet { it.copy(isSaving = true, errors = emptyMap(), failure = null) }
                    viewModelScope.launch {
                        when (val result = repository.saveVehicle(form.vehicleId, validated.value)) {
                            is ApiResult.Success ->
                                _state.update { it.copy(vehicle = result.value, vehicleSheet = null) }
                            else ->
                                updateVehicleSheet { it.copy(isSaving = false, failure = result.failureOrNull()) }
                        }
                    }
                }
            }
        }

        override fun closeVehicleSheet() {
            _state.update { it.copy(vehicleSheet = null, showMeasureHelp = false) }
        }

        override fun setMeasureHelpVisible(visible: Boolean) {
            _state.update { it.copy(showMeasureHelp = visible) }
        }

        override fun openPricesSheet() {
            _state.update { it.copy(pricesSheet = PricesFormState.from(it.prices)) }
        }

        override fun onPriceChange(
            fuelId: String,
            value: String,
        ) {
            updatePricesSheet {
                it.copy(values = it.values + (fuelId to value), errors = emptyMap(), failure = null)
            }
        }

        override fun savePrices() {
            val form = _state.value.pricesSheet ?: return
            if (form.isSaving) return
            when (val validated = ProfileValidation.validatePrices(form)) {
                is Validated.Invalid -> updatePricesSheet { it.copy(errors = validated.errors) }
                is Validated.Valid -> {
                    updatePricesSheet { it.copy(isSaving = true, errors = emptyMap(), failure = null) }
                    viewModelScope.launch {
                        when (val result = repository.updatePrices(validated.value)) {
                            is ApiResult.Success ->
                                _state.update { it.copy(prices = result.value.toPriceMap(), pricesSheet = null) }
                            else ->
                                updatePricesSheet { it.copy(isSaving = false, failure = result.failureOrNull()) }
                        }
                    }
                }
            }
        }

        override fun closePricesSheet() {
            _state.update { it.copy(pricesSheet = null) }
        }

        override fun logout() {
            sessionManager.logout()
        }

        private fun updateVehicleSheet(transform: (VehicleFormState) -> VehicleFormState) {
            _state.update { state -> state.vehicleSheet?.let { state.copy(vehicleSheet = transform(it)) } ?: state }
        }

        private fun updatePricesSheet(transform: (PricesFormState) -> PricesFormState) {
            _state.update { state -> state.pricesSheet?.let { state.copy(pricesSheet = transform(it)) } ?: state }
        }

        private fun List<FuelPriceResponse>.toPriceMap() = associate { it.fuelId to it.pricePerLiter }
    }
