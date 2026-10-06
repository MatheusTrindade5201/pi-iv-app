package br.univesp.pi.expedientes.core.network

import br.univesp.pi.expedientes.core.network.serialization.Day
import br.univesp.pi.expedientes.core.network.serialization.Decimal
import br.univesp.pi.expedientes.core.network.serialization.Timestamp
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class SerializersTest {
    @Serializable
    data class Sample(
        val amount: Decimal,
        val optional: Decimal? = null,
        val at: Timestamp,
        val day: Day,
    )

    @Test
    fun decodesApiFormats() {
        val sample =
            ApiJson.decodeFromString(
                Sample.serializer(),
                """{"amount":"187.40","optional":null,"at":"2026-09-28T14:59:56.123456Z","day":"2026-09-27"}""",
            )

        assertEquals(BigDecimal("187.40"), sample.amount)
        assertEquals(null, sample.optional)
        assertEquals(Instant.parse("2026-09-28T14:59:56.123456Z"), sample.at)
        assertEquals(LocalDate.of(2026, 9, 27), sample.day)
    }

    @Test
    fun decimalKeepsScaleAndNeverUsesScientificNotation() {
        val sample =
            Sample(
                amount = BigDecimal("0.10"),
                optional = BigDecimal("1E+3"),
                at = Instant.parse("2026-01-01T00:00:00Z"),
                day = LocalDate.of(2026, 1, 1),
            )

        val json = ApiJson.encodeToString(Sample.serializer(), sample)

        assertEquals(
            """{"amount":"0.10","optional":"1000","at":"2026-01-01T00:00:00Z","day":"2026-01-01"}""",
            json,
        )
        assertEquals(BigDecimal("0.10"), ApiJson.decodeFromString(Sample.serializer(), json).amount)
    }

    @Test
    fun timestampWithOffsetIsAccepted() {
        val sample =
            ApiJson.decodeFromString(
                Sample.serializer(),
                """{"amount":"1","at":"2026-09-28T11:59:56-03:00","day":"2026-09-27"}""",
            )

        assertEquals(Instant.parse("2026-09-28T14:59:56Z"), sample.at)
    }
}
