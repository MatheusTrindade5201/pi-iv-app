package br.univesp.pi.expedientes.core.network.api

import br.univesp.pi.expedientes.core.network.serialization.Day
import br.univesp.pi.expedientes.core.network.serialization.Decimal
import br.univesp.pi.expedientes.core.network.serialization.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
enum class ShiftStatus {
    @SerialName("in_progress")
    InProgress,

    @SerialName("finished")
    Finished,
}

@Serializable
data class ShiftStartRequest(
    @SerialName("vehicle_id") val vehicleId: String,
    @SerialName("fuel_id") val fuelId: String? = null,
)

@Serializable
data class ShiftSummary(
    @SerialName("earnings_total") val earningsTotal: Decimal,
    @SerialName("fuel_cost") val fuelCost: Decimal,
    val profit: Decimal,
    @SerialName("distance_km") val distanceKm: Decimal,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("idle_minutes") val idleMinutes: Int,
    @SerialName("profit_per_hour") val profitPerHour: Decimal?,
    @SerialName("profit_per_km") val profitPerKm: Decimal?,
)

@Serializable
data class GpsGap(
    val start: Timestamp,
    val end: Timestamp,
    val minutes: Int,
)

@Serializable
data class GpsStatus(
    @SerialName("last_point_at") val lastPointAt: Timestamp?,
    val gaps: List<GpsGap>,
)

@Serializable
data class ShiftResponse(
    val id: String,
    val status: ShiftStatus,
    @SerialName("vehicle_id") val vehicleId: String,
    @SerialName("fuel_id") val fuelId: String,
    @SerialName("started_at") val startedAt: Timestamp,
    @SerialName("ended_at") val endedAt: Timestamp?,
    @SerialName("price_per_liter_used") val pricePerLiterUsed: Decimal,
    @SerialName("kml_used") val kmlUsed: Decimal,
    val summary: ShiftSummary,
    val gps: GpsStatus,
)

@Serializable
data class ShiftListItem(
    val id: String,
    val status: ShiftStatus,
    @SerialName("vehicle_id") val vehicleId: String,
    @SerialName("fuel_id") val fuelId: String,
    @SerialName("started_at") val startedAt: Timestamp,
    @SerialName("ended_at") val endedAt: Timestamp?,
    @SerialName("price_per_liter_used") val pricePerLiterUsed: Decimal,
    @SerialName("kml_used") val kmlUsed: Decimal,
    val summary: ShiftSummary?,
)

@Serializable
data class ShiftPage(
    val items: List<ShiftListItem>,
    val total: Int,
    val page: Int,
    val size: Int,
)

interface ShiftApi {
    @POST("shifts")
    suspend fun start(
        @Body body: ShiftStartRequest,
    ): ShiftResponse

    @GET("shifts/current")
    suspend fun current(): ShiftResponse

    @GET("shifts/{id}")
    suspend fun get(
        @Path("id") id: String,
    ): ShiftResponse

    @POST("shifts/{id}/finish")
    suspend fun finish(
        @Path("id") id: String,
    ): ShiftResponse

    @GET("shifts")
    suspend fun list(
        @Query("from") from: Day? = null,
        @Query("to") to: Day? = null,
        @Query("tz") tz: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
    ): ShiftPage
}
