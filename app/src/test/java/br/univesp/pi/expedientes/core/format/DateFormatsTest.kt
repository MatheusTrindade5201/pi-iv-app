package br.univesp.pi.expedientes.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class DateFormatsTest {
    @Test
    fun headerMatchesPrototype() {
        assertEquals("Sábado, 19 de Setembro", homeHeader(LocalDate.of(2026, 9, 19)))
        assertEquals("Segunda-feira, 5 de Outubro", homeHeader(LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun relativeDays() {
        val today = LocalDate.of(2026, 10, 6)

        assertEquals(DayLabel.Today, relativeDay(today, today))
        assertEquals(DayLabel.Yesterday, relativeDay(LocalDate.of(2026, 10, 5), today))
        assertEquals(DayLabel.Date("18/09/2026"), relativeDay(LocalDate.of(2026, 9, 18), today))
    }

    @Test
    fun durations() {
        assertEquals("4h", formatDuration(240))
        assertEquals("3h 42min", formatDuration(222))
        assertEquals("42min", formatDuration(42))
        assertEquals("0min", formatDuration(0))
    }

    @Test
    fun kilometers() {
        assertEquals("78,2", formatKm(BigDecimal("78.200")))
        assertEquals("5,5", formatKm(BigDecimal("5.496")))
        assertEquals("0,0", formatKm(BigDecimal.ZERO))
    }

    @Test
    fun ratesFollowApiRounding() {
        assertEquals(BigDecimal("46.85"), perHour(BigDecimal("187.40"), 240))
        assertEquals(BigDecimal("2.40"), perKm(BigDecimal("187.40"), BigDecimal("78.2")))
        assertEquals(BigDecimal("0.13"), perHour(BigDecimal("0.25"), 120))
        assertNull(perHour(BigDecimal.TEN, 0))
        assertNull(perKm(BigDecimal.TEN, BigDecimal("0.000")))
    }
}
