package br.univesp.pi.expedientes.core.network.api

import br.univesp.pi.expedientes.core.network.serialization.Decimal
import br.univesp.pi.expedientes.core.network.serialization.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class EarningCreateRequest(
    val amount: Decimal,
    val platform: String? = null,
    @SerialName("earned_at") val earnedAt: Timestamp? = null,
)

@Serializable
data class EarningResponse(
    val id: String,
    @SerialName("shift_id") val shiftId: String,
    val amount: Decimal,
    val platform: String?,
    @SerialName("earned_at") val earnedAt: Timestamp,
)

interface EarningApi {
    @POST("shifts/{shiftId}/earnings")
    suspend fun add(
        @Path("shiftId") shiftId: String,
        @Body body: EarningCreateRequest,
    ): EarningResponse

    @GET("shifts/{shiftId}/earnings")
    suspend fun list(
        @Path("shiftId") shiftId: String,
    ): List<EarningResponse>

    @PATCH("shifts/{shiftId}/earnings/{earningId}")
    suspend fun update(
        @Path("shiftId") shiftId: String,
        @Path("earningId") earningId: String,
        @Body changes: JsonObject,
    ): EarningResponse

    @DELETE("shifts/{shiftId}/earnings/{earningId}")
    suspend fun delete(
        @Path("shiftId") shiftId: String,
        @Path("earningId") earningId: String,
    ): Response<Unit>
}
