package br.univesp.pi.expedientes.core.network

import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.EarningResponse
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.FuelPriceResponse
import br.univesp.pi.expedientes.core.network.api.FuelResponse
import br.univesp.pi.expedientes.core.network.api.HealthResponse
import br.univesp.pi.expedientes.core.network.api.PeriodSummary
import br.univesp.pi.expedientes.core.network.api.ShiftPage
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.network.api.ShiftStatus
import br.univesp.pi.expedientes.core.network.api.TokenResponse
import br.univesp.pi.expedientes.core.network.api.TrackPointBatchResult
import br.univesp.pi.expedientes.core.network.api.TrackPointResponse
import br.univesp.pi.expedientes.core.network.api.UserResponse
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ApiContractTest {
    private fun fixture(name: String): String = Fixtures.read(name)

    private fun <T> decode(
        strategy: DeserializationStrategy<T>,
        name: String,
    ): T = ApiJson.decodeFromString(strategy, fixture(name))

    @Test
    fun health() {
        assertEquals("ok", decode(HealthResponse.serializer(), "health").status)
    }

    @Test
    fun authAndUser() {
        assertEquals("bearer", decode(TokenResponse.serializer(), "login").tokenType)
        listOf("register", "users_me", "users_me_update").forEach {
            assertEquals("John Doe", decode(UserResponse.serializer(), it).name)
        }
    }

    @Test
    fun fuelsAndPrices() {
        val fuels = decode(ListSerializer(FuelResponse.serializer()), "fuels")
        assertEquals(listOf(FuelIds.GASOLINE, FuelIds.ETHANOL, FuelIds.FLEX), fuels.map { it.id })

        val prices = decode(ListSerializer(FuelPriceResponse.serializer()), "fuel_prices_current")
        val gasoline = prices.first { it.fuelId == FuelIds.GASOLINE }
        assertEquals(0, BigDecimal("6.20").compareTo(gasoline.pricePerLiter))
        decode(ListSerializer(FuelPriceResponse.serializer()), "fuel_prices_update")
    }

    @Test
    fun vehicles() {
        val vehicle = decode(VehicleResponse.serializer(), "vehicle")
        assertEquals("Gasolina", vehicle.fuelName)
        assertEquals(BigDecimal("32.00"), vehicle.declaredKml)
        assertEquals(2022, vehicle.year)
        assertNull(decode(VehicleResponse.serializer(), "vehicle_update").year)
        assertEquals(1, decode(ListSerializer(VehicleResponse.serializer()), "vehicles").size)
    }

    @Test
    fun shiftInProgressHasPartialSummaryAndGps() {
        val shift = decode(ShiftResponse.serializer(), "shift_current")
        assertEquals(ShiftStatus.InProgress, shift.status)
        assertNull(shift.endedAt)
        assertEquals(BigDecimal("5.496"), shift.summary.distanceKm)
        assertEquals(20, shift.summary.idleMinutes)
        assertNotNull(shift.gps.lastPointAt)
        assertEquals(listOf(5, 15, 10, 30, 10), shift.gps.gaps.map { it.minutes })
        assertEquals(ShiftStatus.InProgress, decode(ShiftResponse.serializer(), "shift_started").status)
    }

    @Test
    fun finishedShiftAndList() {
        val finished = decode(ShiftResponse.serializer(), "shift_finished")
        assertEquals(ShiftStatus.Finished, finished.status)
        assertNotNull(finished.endedAt)
        assertEquals(finished.summary, decode(ShiftResponse.serializer(), "shift_detail").summary)

        val page = decode(ShiftPage.serializer(), "shifts_page")
        assertEquals(1, page.total)
        assertEquals(
            finished.summary.profit,
            page.items
                .single()
                .summary
                ?.profit,
        )
    }

    @Test
    fun earnings() {
        val updated = decode(EarningResponse.serializer(), "earning_update")
        assertEquals(BigDecimal("8.00"), updated.amount)
        assertEquals("iFood", updated.platform)
        val all = decode(ListSerializer(EarningResponse.serializer()), "earnings")
        assertEquals(2, all.size)
        assertNull(all.last().platform)
    }

    @Test
    fun trackPoints() {
        val result = decode(TrackPointBatchResult.serializer(), "track_points_send")
        assertEquals(TrackPointBatchResult(accepted = 5, duplicated = 0, rejected = 0), result)
        val points = decode(ListSerializer(TrackPointResponse.serializer()), "track_points")
        assertEquals(5, points.size)
        assertTrue(points.zipWithNext().all { (a, b) -> a.recordedAt < b.recordedAt })
    }

    @Test
    fun analytics() {
        val days = decode(ListSerializer(DailyTotals.serializer()), "analytics_daily")
        val summary = decode(PeriodSummary.serializer(), "analytics_summary")
        assertEquals(summary.profit, days.sumOf { it.profit })
        assertTrue(summary.from <= summary.to)
        assertEquals(1, summary.daysWorked)
    }

    @Test
    fun errorBodiesBecomeMessages() {
        assertEquals("E-mail já cadastrado", errorMessage(fixture("error_conflict"), 409))
        assertEquals("E-mail ou senha inválidos", errorMessage(fixture("error_unauthorized"), 401))
        assertEquals("Não autenticado", errorMessage(fixture("error_unauthenticated"), 401))
        assertEquals("Veículo não encontrado", errorMessage(fixture("error_not_found"), 404))
        assertEquals("Expediente já encerrado", errorMessage(fixture("error_shift_finished"), 409))
        assertEquals(
            "Dados inválidos: name, email, password",
            errorMessage(fixture("error_validation"), 422),
        )
    }
}
