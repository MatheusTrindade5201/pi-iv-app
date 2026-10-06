package br.univesp.pi.expedientes.core.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val ptBr = Locale.forLanguageTag("pt-BR")

private fun moneyFormat(): DecimalFormat =
    DecimalFormat("#,##0.00", DecimalFormatSymbols(ptBr)).apply { roundingMode = RoundingMode.HALF_UP }

fun formatBrl(value: BigDecimal): String {
    val text = "R$ " + moneyFormat().format(value.abs())
    return if (value.signum() < 0) "-$text" else text
}

fun formatSignedBrl(value: BigDecimal): String = if (value.signum() < 0) formatBrl(value) else "+${formatBrl(value)}"

fun formatDecimal(value: BigDecimal): String {
    val plain = value.stripTrailingZeros().toPlainString()
    return plain.replace('.', ',')
}

fun formatMoneyInput(value: BigDecimal): String = moneyFormat().format(value).replace(".", "")

fun parseDecimal(text: String): BigDecimal? {
    val cleaned =
        text
            .replace("R$", "")
            .replace(" ", "")
            .replace(" ", "")
            .trim()
    if (cleaned.isEmpty()) return null
    val normalized =
        when {
            cleaned.contains(',') && cleaned.contains('.') -> cleaned.replace(".", "").replace(',', '.')
            cleaned.contains(',') -> cleaned.replace(',', '.')
            else -> cleaned
        }
    if (!normalized.matches(Regex("^-?\\d+(\\.\\d+)?$"))) return null
    return normalized.toBigDecimal()
}
