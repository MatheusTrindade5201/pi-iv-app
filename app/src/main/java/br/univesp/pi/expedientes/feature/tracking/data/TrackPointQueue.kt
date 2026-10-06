package br.univesp.pi.expedientes.feature.tracking.data

import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.api.TrackPointApi
import br.univesp.pi.expedientes.core.network.api.TrackPointBatchRequest
import br.univesp.pi.expedientes.core.network.api.TrackPointInput
import br.univesp.pi.expedientes.core.network.apiCall
import br.univesp.pi.expedientes.feature.tracking.LocationFix
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class FlushResult {
    Done,
    Retry,
    Unauthorized,
}

interface TrackPointSink {
    suspend fun enqueue(
        shiftId: String,
        fix: LocationFix,
    )

    suspend fun flush(): FlushResult
}

@Singleton
class TrackPointQueue
    @Inject
    constructor(
        private val dao: TrackPointDao,
        private val api: TrackPointApi,
    ) : TrackPointSink {
        override suspend fun enqueue(
            shiftId: String,
            fix: LocationFix,
        ) {
            dao.insert(
                TrackPointEntity(
                    shiftId = shiftId,
                    recordedAtMillis = fix.recordedAt.toEpochMilli(),
                    latitude = fix.latitude,
                    longitude = fix.longitude,
                    accuracyM = fix.accuracyM,
                    speedMs = fix.speedMs,
                ),
            )
        }

        override suspend fun flush(): FlushResult {
            while (true) {
                val shiftId = dao.oldest(1).firstOrNull()?.shiftId ?: return FlushResult.Done
                val batch = dao.oldestForShift(shiftId, BATCH_SIZE)
                val result = apiCall { api.send(shiftId, TrackPointBatchRequest(batch.map { it.toInput() })) }
                when {
                    result is ApiResult.Success -> dao.delete(batch.map { it.id })
                    result is ApiResult.HttpError && result.code in DISCARD_CODES -> dao.delete(batch.map { it.id })
                    result is ApiResult.HttpError && result.code == 401 -> return FlushResult.Unauthorized
                    else -> return FlushResult.Retry
                }
            }
        }

        suspend fun pending(shiftId: String): Int = dao.count(shiftId)

        suspend fun clear() = dao.clear()

        private fun TrackPointEntity.toInput() =
            TrackPointInput(
                recordedAt = Instant.ofEpochMilli(recordedAtMillis),
                latitude = latitude,
                longitude = longitude,
                accuracyM = accuracyM,
                speedMs = speedMs,
            )

        companion object {
            const val BATCH_SIZE = 500
            private val DISCARD_CODES = setOf(404, 409, 422)
        }
    }
