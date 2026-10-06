package br.univesp.pi.expedientes.feature.home

import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.api.GpsStatus
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ShiftClockTest {
    private val shift = ApiJson.decodeFromString(ShiftResponse.serializer(), Fixtures.read("shift_current"))
    private val apiLast = Instant.parse("2026-10-06T09:21:06.208917Z")

    @Test
    fun lastPointIsTheNewestBetweenApiAndPhone() {
        assertEquals(apiLast, lastPointAt(shift, null))
        assertEquals(apiLast, lastPointAt(shift, apiLast.minusSeconds(30)))
        assertEquals(apiLast.plusSeconds(4), lastPointAt(shift, apiLast.plusSeconds(4)))
    }

    @Test
    fun bannerStates() {
        val now = apiLast.plusSeconds(4)
        assertEquals(GpsBanner.Active(4), gpsBanner(running = true, lastPoint = apiLast, now = now))
        assertEquals(GpsBanner.Active(0), gpsBanner(true, apiLast.plusSeconds(10), now))
        assertEquals(GpsBanner.Waiting, gpsBanner(true, null, now))
        assertEquals(GpsBanner.Inactive(121, canResume = false), gpsBanner(true, apiLast, apiLast.plusSeconds(121)))
        assertEquals(GpsBanner.Inactive(4, canResume = true), gpsBanner(false, apiLast, now))
        assertEquals(GpsBanner.Inactive(null, canResume = true), gpsBanner(false, null, now))
    }

    @Test
    fun elapsedUsesStartAndNow() {
        assertEquals(222, elapsedMinutes(shift, shift.startedAt.plusSeconds(222 * 60 + 59)))
        assertEquals(0, elapsedMinutes(shift, shift.startedAt.minusSeconds(5)))
    }

    @Test
    fun inactiveMinutesAddsGapsAndSilenceSinceLastPoint() {
        assertEquals(70, inactiveMinutes(shift, null, apiLast.plusSeconds(120)))
        assertEquals(76, inactiveMinutes(shift, null, apiLast.plusSeconds(6 * 60)))
        assertEquals(70, inactiveMinutes(shift, apiLast.plusSeconds(5 * 60), apiLast.plusSeconds(6 * 60)))

        val noPoints = shift.copy(gps = GpsStatus(null, emptyList()))
        assertEquals(6, inactiveMinutes(noPoints, null, shift.startedAt.plusSeconds(6 * 60 + 30)))
        assertEquals(0, inactiveMinutes(noPoints, null, shift.startedAt.plusSeconds(60)))
    }

    @Test
    fun agoText() {
        assertEquals("4s", formatAgo(4))
        assertEquals("59s", formatAgo(59))
        assertEquals("6 min", formatAgo(6 * 60 + 20))
    }
}
