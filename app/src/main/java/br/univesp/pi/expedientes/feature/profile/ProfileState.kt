package br.univesp.pi.expedientes.feature.profile

import br.univesp.pi.expedientes.core.format.formatDecimal
import br.univesp.pi.expedientes.core.format.formatMoneyInput
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import java.math.BigDecimal

data class VehicleFormState(
    val vehicleId: String? = null,
    val model: String = "",
    val fuelId: String = FuelIds.GASOLINE,
    val kml: String = "",
    val displacement: String = "",
    val year: String = "",
    val errors: Map<VehicleField, ProfileError> = emptyMap(),
    val failure: RequestFailure? = null,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = vehicleId != null

    fun edit(
        field: VehicleField,
        value: String,
    ): VehicleFormState {
        val updated =
            when (field) {
                VehicleField.Model -> copy(model = value)
                VehicleField.Kml -> copy(kml = value)
                VehicleField.Displacement -> copy(displacement = value)
                VehicleField.Year -> copy(year = value)
            }
        return updated.copy(errors = errors - field, failure = null)
    }

    companion object {
        fun from(vehicle: VehicleResponse?): VehicleFormState =
            if (vehicle == null) {
                VehicleFormState()
            } else {
                VehicleFormState(
                    vehicleId = vehicle.id,
                    model = vehicle.model,
                    fuelId = vehicle.fuelId,
                    kml = formatDecimal(vehicle.declaredKml),
                    displacement = vehicle.displacementCc?.toString().orEmpty(),
                    year = vehicle.year?.toString().orEmpty(),
                )
            }
    }
}

data class PricesFormState(
    val values: Map<String, String> = emptyMap(),
    val errors: Map<String, ProfileError> = emptyMap(),
    val failure: RequestFailure? = null,
    val isSaving: Boolean = false,
) {
    companion object {
        fun from(prices: Map<String, BigDecimal>): PricesFormState =
            PricesFormState(values = prices.mapValues { formatMoneyInput(it.value) })
    }
}

sealed interface NameStatus {
    data object Idle : NameStatus

    data object Saving : NameStatus

    data object Saved : NameStatus

    data class Invalid(
        val error: ProfileError,
    ) : NameStatus

    data class Failed(
        val failure: RequestFailure,
    ) : NameStatus
}

data class ProfileUiState(
    val isLoading: Boolean = true,
    val loadFailure: RequestFailure? = null,
    val name: String = "",
    val savedName: String = "",
    val nameStatus: NameStatus = NameStatus.Idle,
    val vehicle: VehicleResponse? = null,
    val prices: Map<String, BigDecimal> = emptyMap(),
    val vehicleSheet: VehicleFormState? = null,
    val pricesSheet: PricesFormState? = null,
    val showMeasureHelp: Boolean = false,
)
