package br.univesp.pi.expedientes.core.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class HealthResponse(
    val status: String,
    val database: String,
)

interface HealthApi {
    @GET("health")
    suspend fun health(): HealthResponse
}
