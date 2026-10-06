package br.univesp.pi.expedientes.feature.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

interface LocationSource {
    fun fixes(): Flow<LocationFix>
}

object LocationPermissions {
    val location = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    val requested: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            location + Manifest.permission.POST_NOTIFICATIONS
        } else {
            location
        }

    fun hasPreciseLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}

class FusedLocationSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LocationSource {
        override fun fixes(): Flow<LocationFix> =
            callbackFlow {
                val granted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    close()
                    return@callbackFlow
                }
                val client = LocationServices.getFusedLocationProviderClient(context)
                val request =
                    LocationRequest
                        .Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
                        .setMinUpdateIntervalMillis(INTERVAL_MS)
                        .build()
                val callback =
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            result.locations.forEach { trySend(LocationFix.from(it)) }
                        }
                    }
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                awaitClose { client.removeLocationUpdates(callback) }
            }

        private companion object {
            const val INTERVAL_MS = 5_000L
        }
    }
