package br.univesp.pi.expedientes.feature.tracking

import br.univesp.pi.expedientes.feature.tracking.data.FlushResult
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointSink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingRecorderTest {
    private val fixes = Channel<LocationFix>(Channel.UNLIMITED)
    private val source =
        object : LocationSource {
            override fun fixes() = this@TrackingRecorderTest.fixes.receiveAsFlow()
        }
    private val sink =
        object : TrackPointSink {
            val enqueued = mutableListOf<Pair<String, LocationFix>>()
            var flushes = 0
            var result = FlushResult.Done

            override suspend fun enqueue(
                shiftId: String,
                fix: LocationFix,
            ) {
                enqueued += shiftId to fix
            }

            override suspend fun flush(): FlushResult {
                flushes++
                return result
            }
        }
    private var scheduled = 0
    private val scheduler =
        object : UploadScheduler {
            override fun schedule() {
                scheduled++
            }
        }
    private val status = TrackingStatus()
    private val recorder = TrackingRecorder(source, sink, scheduler, status)

    private fun fix(second: Long) = LocationFix(Instant.EPOCH.plusSeconds(second), -23.5, -46.6)

    private suspend fun TestScope.record(block: suspend TestScope.() -> Unit) {
        val job = launch { recorder.run("s1") }
        runCurrent()
        block()
        job.cancel()
    }

    @Test
    fun enqueuesEveryFixAndTracksLastOne() =
        runTest {
            record {
                fixes.send(fix(0))
                fixes.send(fix(5))
                runCurrent()

                assertEquals(listOf("s1" to fix(0), "s1" to fix(5)), sink.enqueued)
                assertEquals(fix(5).recordedAt, status.lastFixAt.value)
                assertEquals(0, sink.flushes)
            }
        }

    @Test
    fun flushesEveryFiftyPoints() =
        runTest {
            record {
                repeat(49) { fixes.send(fix(it.toLong())) }
                runCurrent()
                assertEquals(0, sink.flushes)

                fixes.send(fix(49))
                runCurrent()
                assertEquals(1, sink.flushes)
            }
        }

    @Test
    fun flushesEveryThirtySeconds() =
        runTest {
            record {
                advanceTimeBy(TrackingRecorder.FLUSH_INTERVAL_MS - 1)
                runCurrent()
                assertEquals(0, sink.flushes)

                advanceTimeBy(2)
                runCurrent()
                assertEquals(1, sink.flushes)

                advanceTimeBy(TrackingRecorder.FLUSH_INTERVAL_MS)
                runCurrent()
                assertEquals(2, sink.flushes)
            }
        }

    @Test
    fun retrySchedulesBackgroundUpload() =
        runTest {
            sink.result = FlushResult.Retry
            record {
                advanceTimeBy(TrackingRecorder.FLUSH_INTERVAL_MS + 1)
                runCurrent()

                assertEquals(1, scheduled)
            }
        }

    @Test
    fun stoppingClearsLastFix() {
        status.running(true)
        status.fixReceived(Instant.EPOCH)

        status.running(false)

        assertEquals(false, status.isRunning.value)
        assertEquals(null, status.lastFixAt.value)
    }
}
