package br.univesp.pi.expedientes.core.network

import br.univesp.pi.expedientes.core.network.api.HealthApi
import javax.inject.Inject

enum class ApiStatus {
    Connected,
    Unavailable,
}

interface HealthRepository {
    suspend fun check(): ApiStatus
}

class DefaultHealthRepository
    @Inject
    constructor(
        private val api: HealthApi,
    ) : HealthRepository {
        override suspend fun check(): ApiStatus =
            when (val result = apiCall { api.health() }) {
                is ApiResult.Success -> if (result.value.status == "ok") ApiStatus.Connected else ApiStatus.Unavailable
                else -> ApiStatus.Unavailable
            }
    }
