package br.univesp.pi.expedientes.feature.tracking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {
    @Inject lateinit var recorder: TrackingRecorder

    @Inject lateinit var store: TrackingStore

    @Inject lateinit var status: TrackingStatus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recording: Job? = null
    private var currentShiftId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        TrackingNotification.ensureChannel(this)
        ServiceCompat.startForeground(
            this,
            TrackingNotification.NOTIFICATION_ID,
            TrackingNotification.build(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
        )
        val requested = intent?.getStringExtra(EXTRA_SHIFT_ID)
        scope.launch {
            val shiftId = requested ?: store.activeShiftId()
            if (shiftId == null || !LocationPermissions.hasPreciseLocation(this@LocationTrackingService)) {
                stopSelf()
                return@launch
            }
            store.setActiveShift(shiftId)
            startRecording(shiftId)
        }
        return START_STICKY
    }

    private fun startRecording(shiftId: String) {
        if (currentShiftId == shiftId && recording?.isActive == true) return
        recording?.cancel()
        currentShiftId = shiftId
        status.running(true)
        recording = scope.launch { recorder.run(shiftId) }
    }

    override fun onDestroy() {
        status.running(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_SHIFT_ID = "shift_id"

        fun startIntent(
            context: Context,
            shiftId: String,
        ): Intent = Intent(context, LocationTrackingService::class.java).putExtra(EXTRA_SHIFT_ID, shiftId)

        fun stopIntent(context: Context): Intent = Intent(context, LocationTrackingService::class.java)
    }
}
