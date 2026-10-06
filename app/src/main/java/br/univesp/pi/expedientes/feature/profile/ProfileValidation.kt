package br.univesp.pi.expedientes.feature.profile

import br.univesp.pi.expedientes.core.format.parseDecimal
import br.univesp.pi.expedientes.core.network.api.FuelIds
import java.math.BigDecimal

enum class VehicleField {
    Model,
    Kml,
    Displacement,
    Year,
}

enum class ProfileError {
    BlankName,
    LongName,
    BlankModel,
    LongModel,
    InvalidKml,
    InvalidDisplacement,
    InvalidYear,
    InvalidPrice,
    NoPrice,
}

data class VehicleInput(
    val model: String,
    val fuelId: String,
    val declaredKml: BigDecimal,
    val displacementCc: Int?,
    val year: Int?,
)

sealed interface Validated<out T, out E> {
    data class Valid<T>(
        val value: T,
    ) : Validated<T, Nothing>

    data class Invalid<E>(
        val errors: E,
    ) : Validated<Nothing, E>
}

object ProfileValidation {
    const val MAX_TEXT = 120
    const val MIN_YEAR = 1950
    const val MAX_DISPLACEMENT = 3000
    private val LIMIT = BigDecimal(1000)

    fun validateName(name: String): ProfileError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ProfileError.BlankName
            trimmed.length > MAX_TEXT -> ProfileError.LongName
            else -> null
        }
    }

    fun validateVehicle(
        form: VehicleFormState,
        currentYear: Int,
    ): Validated<VehicleInput, Map<VehicleField, ProfileError>> {
        val errors = mutableMapOf<VehicleField, ProfileError>()
        val model = form.model.trim()
        when {
            model.isEmpty() -> errors[VehicleField.Model] = ProfileError.BlankModel
            model.length > MAX_TEXT -> errors[VehicleField.Model] = ProfileError.LongModel
        }
        val kml = positiveDecimal(form.kml, decimals = 2)
        if (kml == null) errors[VehicleField.Kml] = ProfileError.InvalidKml
        val displacement = optionalInt(form.displacement, 1..MAX_DISPLACEMENT)
        if (displacement == null) errors[VehicleField.Displacement] = ProfileError.InvalidDisplacement
        val year = optionalInt(form.year, MIN_YEAR..currentYear + 1)
        if (year == null) errors[VehicleField.Year] = ProfileError.InvalidYear
        if (errors.isNotEmpty() || kml == null || displacement == null || year == null) {
            return Validated.Invalid(errors)
        }
        return Validated.Valid(
            VehicleInput(
                model = model,
                fuelId = form.fuelId,
                declaredKml = kml,
                displacementCc = displacement.value,
                year = year.value,
            ),
        )
    }

    fun validatePrices(form: PricesFormState): Validated<Map<String, BigDecimal>, Map<String, ProfileError>> {
        val errors = mutableMapOf<String, ProfileError>()
        val prices = mutableMapOf<String, BigDecimal>()
        PRICED_FUELS.forEach { fuelId ->
            val text = form.values[fuelId].orEmpty()
            if (text.isBlank()) return@forEach
            val price = positiveDecimal(text, decimals = 3)
            if (price == null) errors[fuelId] = ProfileError.InvalidPrice else prices[fuelId] = price
        }
        if (errors.isEmpty() && prices.isEmpty()) errors[FuelIds.GASOLINE] = ProfileError.NoPrice
        return if (errors.isEmpty()) Validated.Valid(prices) else Validated.Invalid(errors)
    }

    private fun positiveDecimal(
        text: String,
        decimals: Int,
    ): BigDecimal? {
        val value = parseDecimal(text) ?: return null
        val fitsScale = value.stripTrailingZeros().scale() <= decimals
        return value.takeIf { it.signum() > 0 && it < LIMIT && fitsScale }
    }

    private fun optionalInt(
        text: String,
        range: IntRange,
    ): Optional? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Optional(null)
        val value = trimmed.toIntOrNull() ?: return null
        return if (value in range) Optional(value) else null
    }

    private data class Optional(
        val value: Int?,
    )
}

val PRICED_FUELS = listOf(FuelIds.GASOLINE, FuelIds.ETHANOL)
