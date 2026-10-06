package br.univesp.pi.expedientes.testing

import br.univesp.pi.expedientes.feature.tracking.TrackingController
import br.univesp.pi.expedientes.feature.tracking.data.FlushResult

class FakeTrackingController : TrackingController {
    val calls = mutableListOf<String>()

    override suspend fun start(shiftId: String) {
        calls += "start:$shiftId"
    }

    override suspend fun ensureRunning(shiftId: String) {
        calls += "ensure:$shiftId"
    }

    override suspend fun stop() {
        calls += "stop"
    }

    override suspend fun flushNow(): FlushResult {
        calls += "flush"
        return FlushResult.Done
    }

    override suspend fun clearPending() {
        calls += "clear"
    }
}
