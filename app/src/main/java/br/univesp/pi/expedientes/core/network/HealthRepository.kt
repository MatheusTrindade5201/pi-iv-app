package br.univesp.pi.expedientes.core.network

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
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
            try {
                if (api.health().status == "ok") ApiStatus.Connected else ApiStatus.Unavailable
            } catch (_: IOException) {
                ApiStatus.Unavailable
            } catch (_: HttpException) {
                ApiStatus.Unavailable
            } catch (_: SerializationException) {
                ApiStatus.Unavailable
            }
    }
