package br.univesp.pi.expedientes.core.network.api

import br.univesp.pi.expedientes.core.network.serialization.Decimal
import br.univesp.pi.expedientes.core.network.serialization.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class TrackPointInput(
    @SerialName("recorded_at") val recordedAt: Timestamp,
    val latitude: Double,
    val longitude: Double,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
    @SerialName("speed_ms") val speedMs: Double? = null,
)

@Serializable
data class TrackPointBatchRequest(
    val points: List<TrackPointInput>,
)

@Serializable
data class TrackPointBatchResult(
    val accepted: Int,
    val duplicated: Int,
    val rejected: Int,
)

@Serializable
data class TrackPointResponse(
    @SerialName("recorded_at") val recordedAt: Timestamp,
    val latitude: Decimal,
    val longitude: Decimal,
    @SerialName("accuracy_m") val accuracyM: Decimal?,
    @SerialName("speed_ms") val speedMs: Decimal?,
)

interface TrackPointApi {
    @POST("shifts/{shiftId}/track-points")
    suspend fun send(
        @Path("shiftId") shiftId: String,
        @Body body: TrackPointBatchRequest,
    ): TrackPointBatchResult

    @GET("shifts/{shiftId}/track-points")
    suspend fun list(
        @Path("shiftId") shiftId: String,
    ): List<TrackPointResponse>
}
