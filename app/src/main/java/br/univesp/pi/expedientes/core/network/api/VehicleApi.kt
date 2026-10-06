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
data class VehicleCreateRequest(
    val model: String,
    @SerialName("fuel_id") val fuelId: String,
    @SerialName("declared_kml") val declaredKml: Decimal,
    @SerialName("displacement_cc") val displacementCc: Int? = null,
    val year: Int? = null,
)

@Serializable
data class VehicleResponse(
    val id: String,
    val model: String,
    @SerialName("fuel_id") val fuelId: String,
    @SerialName("fuel_name") val fuelName: String,
    @SerialName("declared_kml") val declaredKml: Decimal,
    @SerialName("displacement_cc") val displacementCc: Int?,
    val year: Int?,
    @SerialName("created_at") val createdAt: Timestamp,
)

interface VehicleApi {
    @POST("vehicles")
    suspend fun create(
        @Body body: VehicleCreateRequest,
    ): VehicleResponse

    @GET("vehicles")
    suspend fun list(): List<VehicleResponse>

    @GET("vehicles/{id}")
    suspend fun get(
        @Path("id") id: String,
    ): VehicleResponse

    @PATCH("vehicles/{id}")
    suspend fun update(
        @Path("id") id: String,
        @Body changes: JsonObject,
    ): VehicleResponse

    @DELETE("vehicles/{id}")
    suspend fun delete(
        @Path("id") id: String,
    ): Response<Unit>
}
