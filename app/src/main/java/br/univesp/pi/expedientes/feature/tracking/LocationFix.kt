package br.univesp.pi.expedientes.feature.tracking

import android.location.Location
import java.time.Instant

data class LocationFix(
    val recordedAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double? = null,
    val speedMs: Double? = null,
) {
    companion object {
        fun from(location: Location): LocationFix =
            LocationFix(
                recordedAt = Instant.ofEpochMilli(location.time),
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyM = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
                speedMs = if (location.hasSpeed()) location.speed.toDouble() else null,
            )
    }
}
