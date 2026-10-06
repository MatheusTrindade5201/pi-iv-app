package br.univesp.pi.expedientes.core.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ptBr = Locale.forLanguageTag("pt-BR")
private val headerFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBr)
private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBr)

sealed interface DayLabel {
    data object Today : DayLabel

    data object Yesterday : DayLabel

    data class Date(
        val text: String,
    ) : DayLabel
}

fun homeHeader(date: LocalDate): String =
    headerFormatter
        .format(date)
        .split(" ")
        .joinToString(" ") { word -> if (word == "de") word else word.replaceFirstChar { it.titlecase(ptBr) } }

fun relativeDay(
    date: LocalDate,
    today: LocalDate,
): DayLabel =
    when (date) {
        today -> DayLabel.Today
        today.minusDays(1) -> DayLabel.Yesterday
        else -> DayLabel.Date(dayFormatter.format(date))
    }

fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}min"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}min"
    }
}

fun formatKm(km: BigDecimal): String = km.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
