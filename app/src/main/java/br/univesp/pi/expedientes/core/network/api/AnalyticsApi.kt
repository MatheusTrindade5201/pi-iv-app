package br.univesp.pi.expedientes.core.network.api

import br.univesp.pi.expedientes.core.network.serialization.Day
import br.univesp.pi.expedientes.core.network.serialization.Decimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class DailyTotals(
    val date: Day,
    @SerialName("shifts_count") val shiftsCount: Int,
    @SerialName("earnings_total") val earningsTotal: Decimal,
    @SerialName("fuel_cost") val fuelCost: Decimal,
    val profit: Decimal,
    @SerialName("distance_km") val distanceKm: Decimal,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("idle_minutes") val idleMinutes: Int,
)

@Serializable
data class PeriodSummary(
    @SerialName("from") val from: Day,
    @SerialName("to") val to: Day,
    @SerialName("days_worked") val daysWorked: Int,
    @SerialName("shifts_count") val shiftsCount: Int,
    @SerialName("earnings_total") val earningsTotal: Decimal,
    @SerialName("fuel_cost") val fuelCost: Decimal,
    val profit: Decimal,
    @SerialName("distance_km") val distanceKm: Decimal,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("idle_minutes") val idleMinutes: Int,
    @SerialName("profit_per_hour") val profitPerHour: Decimal?,
    @SerialName("profit_per_km") val profitPerKm: Decimal?,
    @SerialName("avg_profit_per_day") val avgProfitPerDay: Decimal?,
)

interface AnalyticsApi {
    @GET("analytics/daily")
    suspend fun daily(
        @Query("from") from: Day? = null,
        @Query("to") to: Day? = null,
        @Query("tz") tz: String? = null,
    ): List<DailyTotals>

    @GET("analytics/summary")
    suspend fun summary(
        @Query("from") from: Day? = null,
        @Query("to") to: Day? = null,
        @Query("tz") tz: String? = null,
    ): PeriodSummary
}
