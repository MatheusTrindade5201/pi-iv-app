package br.univesp.pi.expedientes.feature.profile

import br.univesp.pi.expedientes.core.network.api.FuelIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ProfileValidationTest {
    private val year = 2026

    private fun vehicle(
        model: String = "Honda CG 160",
        kml: String = "32",
        displacement: String = "",
        yearText: String = "",
    ) = VehicleFormState(model = model, fuelId = FuelIds.FLEX, kml = kml, displacement = displacement, year = yearText)

    private fun errorsOf(form: VehicleFormState) =
        (ProfileValidation.validateVehicle(form, year) as? Validated.Invalid)?.errors.orEmpty()

    @Test
    fun validVehicleBecomesInput() {
        val result = ProfileValidation.validateVehicle(vehicle(model = " Fan ", kml = "35,5", yearText = "2027"), year)

        assertEquals(
            Validated.Valid(VehicleInput("Fan", FuelIds.FLEX, BigDecimal("35.5"), null, 2027)),
            result,
        )
    }

    @Test
    fun modelRules() {
        assertEquals(ProfileError.BlankModel, errorsOf(vehicle(model = "  "))[VehicleField.Model])
        assertEquals(ProfileError.LongModel, errorsOf(vehicle(model = "x".repeat(121)))[VehicleField.Model])
    }

    @Test
    fun consumptionRules() {
        listOf("", "0", "-1", "1000", "32,555", "abc").forEach {
            assertEquals(it, ProfileError.InvalidKml, errorsOf(vehicle(kml = it))[VehicleField.Kml])
        }
        assertNull(errorsOf(vehicle(kml = "999,99"))[VehicleField.Kml])
    }

    @Test
    fun optionalNumbersRules() {
        assertEquals(ProfileError.InvalidDisplacement, errorsOf(vehicle(displacement = "0"))[VehicleField.Displacement])
        assertEquals(
            ProfileError.InvalidDisplacement,
            errorsOf(vehicle(displacement = "3001"))[VehicleField.Displacement],
        )
        assertEquals(ProfileError.InvalidYear, errorsOf(vehicle(yearText = "1949"))[VehicleField.Year])
        assertEquals(ProfileError.InvalidYear, errorsOf(vehicle(yearText = "2028"))[VehicleField.Year])
        assertEquals(ProfileError.InvalidYear, errorsOf(vehicle(yearText = "20a6"))[VehicleField.Year])
    }

    @Test
    fun nameRules() {
        assertEquals(ProfileError.BlankName, ProfileValidation.validateName("  "))
        assertEquals(ProfileError.LongName, ProfileValidation.validateName("x".repeat(121)))
        assertNull(ProfileValidation.validateName(" John "))
    }

    @Test
    fun pricesSendOnlyFilledValues() {
        val result = ProfileValidation.validatePrices(PricesFormState(values = mapOf(FuelIds.GASOLINE to "R$ 6,20")))

        assertEquals(Validated.Valid(mapOf(FuelIds.GASOLINE to BigDecimal("6.20"))), result)
    }

    @Test
    fun pricesRules() {
        assertEquals(
            Validated.Invalid(mapOf(FuelIds.GASOLINE to ProfileError.NoPrice)),
            ProfileValidation.validatePrices(PricesFormState()),
        )
        assertEquals(
            Validated.Invalid(mapOf(FuelIds.ETHANOL to ProfileError.InvalidPrice)),
            ProfileValidation.validatePrices(
                PricesFormState(values = mapOf(FuelIds.GASOLINE to "6,20", FuelIds.ETHANOL to "3,7555")),
            ),
        )
    }
}
