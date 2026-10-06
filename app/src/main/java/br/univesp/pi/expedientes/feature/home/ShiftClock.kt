package br.univesp.pi.expedientes.feature.home

import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import java.time.Duration
import java.time.Instant

const val GPS_STALE_SECONDS = 120L

sealed interface GpsBanner {
    data class Active(
        val secondsAgo: Long,
    ) : GpsBanner

    data object Waiting : GpsBanner

    data class Inactive(
        val secondsAgo: Long?,
        val canResume: Boolean,
    ) : GpsBanner
}

fun lastPointAt(
    shift: ShiftResponse,
    localFix: Instant?,
): Instant? = listOfNotNull(shift.gps.lastPointAt, localFix).maxOrNull()

fun gpsBanner(
    running: Boolean,
    lastPoint: Instant?,
    now: Instant,
): GpsBanner {
    val ago = lastPoint?.let { Duration.between(it, now).seconds.coerceAtLeast(0) }
    return when {
        running && ago != null && ago <= GPS_STALE_SECONDS -> GpsBanner.Active(ago)
        running && ago == null -> GpsBanner.Waiting
        else -> GpsBanner.Inactive(ago, canResume = !running)
    }
}

fun elapsedMinutes(
    shift: ShiftResponse,
    now: Instant,
): Int =
    Duration
        .between(shift.startedAt, now)
        .toMinutes()
        .coerceAtLeast(0)
        .toInt()

fun inactiveMinutes(
    shift: ShiftResponse,
    localFix: Instant?,
    now: Instant,
): Int {
    val trailing = Duration.between(lastPointAt(shift, localFix) ?: shift.startedAt, now).seconds
    val trailingMinutes = if (trailing > GPS_STALE_SECONDS) (trailing / 60).toInt() else 0
    return shift.gps.gaps.sumOf { it.minutes } + trailingMinutes
}

fun formatAgo(seconds: Long): String = if (seconds < 60) "${seconds}s" else "${seconds / 60} min"
