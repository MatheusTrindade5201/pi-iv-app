package br.univesp.pi.expedientes.core.format

import java.math.BigDecimal
import java.math.RoundingMode

fun perHour(
    value: BigDecimal,
    minutes: Int,
): BigDecimal? =
    if (minutes == 0) null else (value * BigDecimal(60)).divide(BigDecimal(minutes), 2, RoundingMode.HALF_UP)

fun perKm(
    value: BigDecimal,
    km: BigDecimal,
): BigDecimal? = if (km.signum() == 0) null else value.divide(km, 2, RoundingMode.HALF_UP)
