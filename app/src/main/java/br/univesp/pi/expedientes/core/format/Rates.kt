package br.univesp.pi.expedientes.core.format

import java.math.BigDecimal
import java.math.RoundingMode

// Mesma regra da API: divide por no mínimo 1 hora, sem projetar expedientes curtos.
fun perHour(
    value: BigDecimal,
    minutes: Int,
): BigDecimal? =
    if (minutes == 0) {
        null
    } else {
        (value * BigDecimal(60)).divide(BigDecimal(maxOf(minutes, 60)), 2, RoundingMode.HALF_UP)
    }

fun perKm(
    value: BigDecimal,
    km: BigDecimal,
): BigDecimal? = if (km.signum() == 0) null else value.divide(km, 2, RoundingMode.HALF_UP)
