package br.univesp.pi.expedientes.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {
    @Test
    fun formatsBrazilianCurrency() {
        assertEquals("R$ 6,20", formatBrl(BigDecimal("6.200")))
        assertEquals("R$ 187,40", formatBrl(BigDecimal("187.4")))
        assertEquals("R$ 1.234,56", formatBrl(BigDecimal("1234.56")))
        assertEquals("R$ 0,00", formatBrl(BigDecimal.ZERO))
        assertEquals("-R$ 6,00", formatBrl(BigDecimal("-6")))
    }

    @Test
    fun roundsHalfUpLikeTheApi() {
        assertEquals("R$ 0,13", formatBrl(BigDecimal("0.125")))
    }

    @Test
    fun signedCurrency() {
        assertEquals("+R$ 187,40", formatSignedBrl(BigDecimal("187.40")))
        assertEquals("+R$ 0,00", formatSignedBrl(BigDecimal.ZERO))
        assertEquals("-R$ 1,36", formatSignedBrl(BigDecimal("-1.36")))
    }

    @Test
    fun decimalWithoutTrailingZeros() {
        assertEquals("32", formatDecimal(BigDecimal("32.00")))
        assertEquals("32,5", formatDecimal(BigDecimal("32.50")))
        assertEquals("1000", formatDecimal(BigDecimal("1E+3")))
    }

    @Test
    fun moneyInputUsesCommaWithoutThousands() {
        assertEquals("6,20", formatMoneyInput(BigDecimal("6.200")))
        assertEquals("1234,50", formatMoneyInput(BigDecimal("1234.5")))
    }

    @Test
    fun parsesUserInput() {
        assertEquals(BigDecimal("6.20"), parseDecimal("6,20"))
        assertEquals(BigDecimal("6.20"), parseDecimal("R$ 6,20"))
        assertEquals(BigDecimal("32.5"), parseDecimal(" 32.5 "))
        assertEquals(BigDecimal("1234.56"), parseDecimal("1.234,56"))
        assertEquals(BigDecimal("32"), parseDecimal("32"))
    }

    @Test
    fun rejectsInvalidInput() {
        listOf("", "  ", "abc", "6,2,0", "6.", ",5", "1e3").forEach { assertNull(it, parseDecimal(it)) }
    }
}
