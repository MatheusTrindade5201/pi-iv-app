package br.univesp.pi.expedientes.feature.tracking

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import br.univesp.pi.expedientes.feature.tracking.data.FlushResult
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointSink
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class TrackingAndroidTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun fixKeepsOptionalFieldsOnlyWhenGpsReportsThem() {
        val bare =
            Location("gps").apply {
                time = 1_759_752_000_000
                latitude = -23.55
                longitude = -46.63
            }
        assertEquals(
            LocationFix(Instant.ofEpochMilli(1_759_752_000_000), -23.55, -46.63, null, null),
            LocationFix.from(bare),
        )

        val full =
            Location(bare).apply {
                accuracy = 8f
                speed = 9.5f
            }
        val fix = LocationFix.from(full)
        assertEquals(8.0, fix.accuracyM)
        assertEquals(9.5, fix.speedMs)
    }

    @Test
    fun notificationIsOngoingOnItsChannel() {
        TrackingNotification.ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        assertNotNull(manager.getNotificationChannel(TrackingNotification.CHANNEL_ID))

        val notification = TrackingNotification.build(context)

        assertEquals(TrackingNotification.CHANNEL_ID, notification.channelId)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertEquals("Expediente em andamento", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Registrando o trajeto pelo GPS", notification.extras.getString(Notification.EXTRA_TEXT))
        assertNotNull(notification.contentIntent)
    }

    private fun worker(result: FlushResult): UploadWorker {
        val sink =
            object : TrackPointSink {
                override suspend fun enqueue(
                    shiftId: String,
                    fix: LocationFix,
                ) = Unit

                override suspend fun flush() = result
            }
        return TestListenableWorkerBuilder<UploadWorker>(context)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = UploadWorker(appContext, workerParameters, sink)
                },
            ).build()
    }

    @Test
    fun workerRetriesOnlyWhenUploadFails() =
        runTest {
            assertEquals(ListenableWorker.Result.success(), worker(FlushResult.Done).doWork())
            assertEquals(ListenableWorker.Result.retry(), worker(FlushResult.Retry).doWork())
            assertEquals(ListenableWorker.Result.success(), worker(FlushResult.Unauthorized).doWork())
        }

    @Test
    fun deniedPermissionPrompt() {
        assertEquals(PermissionPrompt.Ask, permissionPromptAfterDenial(shouldShowRationale = true))
        assertEquals(PermissionPrompt.OpenSettings, permissionPromptAfterDenial(shouldShowRationale = false))
    }

    @Test
    fun withoutPermissionHasNoPreciseLocation() {
        assertEquals(false, LocationPermissions.hasPreciseLocation(context))
    }
}
