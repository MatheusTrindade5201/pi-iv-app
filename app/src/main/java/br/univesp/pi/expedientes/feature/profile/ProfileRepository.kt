package br.univesp.pi.expedientes.feature.profile

import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.api.FuelApi
import br.univesp.pi.expedientes.core.network.api.FuelPriceInput
import br.univesp.pi.expedientes.core.network.api.FuelPriceResponse
import br.univesp.pi.expedientes.core.network.api.FuelPricesUpdateRequest
import br.univesp.pi.expedientes.core.network.api.UserApi
import br.univesp.pi.expedientes.core.network.api.UserResponse
import br.univesp.pi.expedientes.core.network.api.UserUpdateRequest
import br.univesp.pi.expedientes.core.network.api.VehicleApi
import br.univesp.pi.expedientes.core.network.api.VehicleCreateRequest
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import br.univesp.pi.expedientes.core.network.apiCall
import br.univesp.pi.expedientes.core.network.asFailure
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.math.BigDecimal
import javax.inject.Inject

data class ProfileData(
    val user: UserResponse,
    val vehicle: VehicleResponse?,
    val prices: List<FuelPriceResponse>,
)

interface ProfileRepository {
    suspend fun load(): ApiResult<ProfileData>

    suspend fun updateName(name: String): ApiResult<UserResponse>

    suspend fun saveVehicle(
        vehicleId: String?,
        input: VehicleInput,
    ): ApiResult<VehicleResponse>

    suspend fun updatePrices(prices: Map<String, BigDecimal>): ApiResult<List<FuelPriceResponse>>
}

class DefaultProfileRepository
    @Inject
    constructor(
        private val userApi: UserApi,
        private val vehicleApi: VehicleApi,
        private val fuelApi: FuelApi,
    ) : ProfileRepository {
        override suspend fun load(): ApiResult<ProfileData> =
            coroutineScope {
                val userCall = async { apiCall { userApi.me() } }
                val vehiclesCall = async { apiCall { vehicleApi.list() } }
                val pricesCall = async { apiCall { fuelApi.currentPrices() } }
                val user = userCall.await()
                val vehicles = vehiclesCall.await()
                val prices = pricesCall.await()
                when {
                    user !is ApiResult.Success -> user.asFailure()
                    vehicles !is ApiResult.Success -> vehicles.asFailure()
                    prices !is ApiResult.Success -> prices.asFailure()
                    else -> ApiResult.Success(ProfileData(user.value, vehicles.value.firstOrNull(), prices.value))
                }
            }

        override suspend fun updateName(name: String): ApiResult<UserResponse> =
            apiCall { userApi.updateMe(UserUpdateRequest(name)) }

        override suspend fun saveVehicle(
            vehicleId: String?,
            input: VehicleInput,
        ): ApiResult<VehicleResponse> =
            if (vehicleId == null) {
                apiCall {
                    vehicleApi.create(
                        VehicleCreateRequest(
                            model = input.model,
                            fuelId = input.fuelId,
                            declaredKml = input.declaredKml,
                            displacementCc = input.displacementCc,
                            year = input.year,
                        ),
                    )
                }
            } else {
                apiCall { vehicleApi.update(vehicleId, input.toChanges()) }
            }

        override suspend fun updatePrices(prices: Map<String, BigDecimal>): ApiResult<List<FuelPriceResponse>> =
            apiCall {
                fuelApi.updatePrices(
                    FuelPricesUpdateRequest(prices.map { (fuelId, price) -> FuelPriceInput(fuelId, price) }),
                )
            }

        private fun VehicleInput.toChanges() =
            buildJsonObject {
                put("model", JsonPrimitive(model))
                put("fuel_id", JsonPrimitive(fuelId))
                put("declared_kml", JsonPrimitive(declaredKml.toPlainString()))
                put("displacement_cc", displacementCc?.let(::JsonPrimitive) ?: JsonNull)
                put("year", year?.let(::JsonPrimitive) ?: JsonNull)
            }
    }
