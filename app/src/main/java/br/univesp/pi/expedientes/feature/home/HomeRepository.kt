package br.univesp.pi.expedientes.feature.home

import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.api.AnalyticsApi
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.ShiftApi
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.network.api.ShiftStartRequest
import br.univesp.pi.expedientes.core.network.api.UserApi
import br.univesp.pi.expedientes.core.network.api.VehicleApi
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import br.univesp.pi.expedientes.core.network.apiCall
import br.univesp.pi.expedientes.core.network.asFailure
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

data class HomeData(
    val userName: String,
    val currentShift: ShiftResponse?,
    val lastDay: DailyTotals?,
)

interface HomeRepository {
    suspend fun load(): ApiResult<HomeData>

    suspend fun firstVehicle(): ApiResult<VehicleResponse?>

    suspend fun startShift(
        vehicleId: String,
        fuelId: String?,
    ): ApiResult<ShiftResponse>
}

class DefaultHomeRepository
    @Inject
    constructor(
        private val userApi: UserApi,
        private val shiftApi: ShiftApi,
        private val analyticsApi: AnalyticsApi,
        private val vehicleApi: VehicleApi,
    ) : HomeRepository {
        override suspend fun load(): ApiResult<HomeData> =
            coroutineScope {
                val userCall = async { apiCall { userApi.me() } }
                val shiftCall = async { currentShift() }
                val daysCall = async { apiCall { analyticsApi.daily() } }
                val user = userCall.await()
                val shift = shiftCall.await()
                val days = daysCall.await()
                when {
                    user !is ApiResult.Success -> user.asFailure()
                    shift !is ApiResult.Success -> shift.asFailure()
                    days !is ApiResult.Success -> days.asFailure()
                    else -> ApiResult.Success(HomeData(user.value.name, shift.value, days.value.firstOrNull()))
                }
            }

        override suspend fun firstVehicle(): ApiResult<VehicleResponse?> =
            when (val result = apiCall { vehicleApi.list() }) {
                is ApiResult.Success -> ApiResult.Success(result.value.firstOrNull())
                else -> result.asFailure()
            }

        override suspend fun startShift(
            vehicleId: String,
            fuelId: String?,
        ): ApiResult<ShiftResponse> = apiCall { shiftApi.start(ShiftStartRequest(vehicleId, fuelId)) }

        private suspend fun currentShift(): ApiResult<ShiftResponse?> =
            when (val result = apiCall { shiftApi.current() }) {
                is ApiResult.Success -> result
                is ApiResult.HttpError -> if (result.code == 404) ApiResult.Success(null) else result
                else -> result
            }
    }
