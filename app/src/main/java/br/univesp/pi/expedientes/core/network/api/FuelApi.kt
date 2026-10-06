package br.univesp.pi.expedientes.core.network.api

import br.univesp.pi.expedientes.core.network.serialization.Decimal
import br.univesp.pi.expedientes.core.network.serialization.Timestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

object FuelIds {
    const val GASOLINE = "00000000-0000-4000-8000-000000000001"
    const val ETHANOL = "00000000-0000-4000-8000-000000000002"
    const val FLEX = "00000000-0000-4000-8000-000000000003"
}

@Serializable
data class FuelResponse(
    val id: String,
    val name: String,
)

@Serializable
data class FuelPriceInput(
    @SerialName("fuel_id") val fuelId: String,
    @SerialName("price_per_liter") val pricePerLiter: Decimal,
)

@Serializable
data class FuelPricesUpdateRequest(
    val prices: List<FuelPriceInput>,
)

@Serializable
data class FuelPriceResponse(
    @SerialName("fuel_id") val fuelId: String,
    @SerialName("fuel_name") val fuelName: String,
    @SerialName("price_per_liter") val pricePerLiter: Decimal,
    @SerialName("valid_from") val validFrom: Timestamp,
)

interface FuelApi {
    @GET("fuels")
    suspend fun fuels(): List<FuelResponse>

    @GET("fuel-prices/current")
    suspend fun currentPrices(): List<FuelPriceResponse>

    @PUT("fuel-prices")
    suspend fun updatePrices(
        @Body body: FuelPricesUpdateRequest,
    ): List<FuelPriceResponse>
}
