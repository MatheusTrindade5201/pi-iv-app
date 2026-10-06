package br.univesp.pi.expedientes.feature.tracking

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.univesp.pi.expedientes.feature.tracking.data.FlushResult
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointQueue
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointSink
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface TrackingController {
    suspend fun start(shiftId: String)

    suspend fun ensureRunning(shiftId: String)

    suspend fun stop()

    suspend fun flushNow(): FlushResult

    suspend fun clearPending()
}

@Singleton
class AndroidTrackingController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: TrackingStore,
        private val status: TrackingStatus,
        private val queue: TrackPointQueue,
        private val scheduler: UploadScheduler,
    ) : TrackingController {
        override suspend fun start(shiftId: String) {
            if (!LocationPermissions.hasPreciseLocation(context)) return
            store.setActiveShift(shiftId)
            ContextCompat.startForegroundService(context, LocationTrackingService.startIntent(context, shiftId))
        }

        override suspend fun ensureRunning(shiftId: String) {
            if (!status.isRunning.value) start(shiftId)
        }

        override suspend fun stop() {
            store.setActiveShift(null)
            context.stopService(LocationTrackingService.stopIntent(context))
            status.running(false)
        }

        override suspend fun flushNow(): FlushResult {
            val result = queue.flush()
            if (result == FlushResult.Retry) scheduler.schedule()
            return result
        }

        override suspend fun clearPending() = queue.clear()
    }

class WorkUploadScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : UploadScheduler {
        override fun schedule() {
            val request =
                OneTimeWorkRequestBuilder<UploadWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        }

        companion object {
            const val UNIQUE_NAME = "track-points-upload"
        }
    }

@HiltWorker
class UploadWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val sink: TrackPointSink,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            when (sink.flush()) {
                FlushResult.Retry -> Result.retry()
                FlushResult.Done, FlushResult.Unauthorized -> Result.success()
            }
    }
