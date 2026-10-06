package br.univesp.pi.expedientes.feature.tracking

import br.univesp.pi.expedientes.feature.tracking.data.FlushResult
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointSink
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface UploadScheduler {
    fun schedule()
}

@Singleton
class TrackingStatus
    @Inject
    constructor() {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _lastFixAt = MutableStateFlow<Instant?>(null)
        val lastFixAt: StateFlow<Instant?> = _lastFixAt.asStateFlow()

        fun running(value: Boolean) {
            _isRunning.value = value
            if (!value) _lastFixAt.value = null
        }

        fun fixReceived(at: Instant) {
            _lastFixAt.value = at
        }
    }

class TrackingRecorder
    @Inject
    constructor(
        private val source: LocationSource,
        private val sink: TrackPointSink,
        private val scheduler: UploadScheduler,
        private val status: TrackingStatus,
    ) {
        private val flushLock = Mutex()

        suspend fun run(shiftId: String) {
            coroutineScope {
                launch {
                    while (true) {
                        delay(FLUSH_INTERVAL_MS)
                        flush()
                    }
                }
                var sinceFlush = 0
                source.fixes().collect { fix ->
                    sink.enqueue(shiftId, fix)
                    status.fixReceived(fix.recordedAt)
                    sinceFlush++
                    if (sinceFlush >= FLUSH_EVERY_POINTS) {
                        sinceFlush = 0
                        launch { flush() }
                    }
                }
            }
        }

        private suspend fun flush() {
            flushLock.withLock {
                if (sink.flush() == FlushResult.Retry) scheduler.schedule()
            }
        }

        companion object {
            const val FLUSH_INTERVAL_MS = 30_000L
            const val FLUSH_EVERY_POINTS = 50
        }
    }
