package br.univesp.pi.expedientes.feature.tracking

import androidx.room.Room
import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.NetworkModule
import br.univesp.pi.expedientes.core.network.api.TrackPointApi
import br.univesp.pi.expedientes.feature.tracking.data.FlushResult
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointQueue
import br.univesp.pi.expedientes.feature.tracking.data.TrackingDatabase
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class TrackPointQueueTest {
    private val server = MockWebServer()
    private lateinit var database: TrackingDatabase
    private lateinit var queue: TrackPointQueue

    private val start = Instant.parse("2026-10-06T12:00:00Z")
    private val accepted = Fixtures.response(202, "track_points_send")

    @Before
    fun setUp() {
        server.start()
        database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TrackingDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val retrofit = NetworkModule.provideRetrofit(server.url("/api/v1/").toString(), OkHttpClient(), ApiJson)
        queue = TrackPointQueue(database.trackPointDao(), retrofit.create(TrackPointApi::class.java))
    }

    @After
    fun tearDown() {
        database.close()
        server.close()
    }

    private fun fix(
        second: Long,
        accuracy: Double? = 8.5,
    ) = LocationFix(start.plusSeconds(second), -23.55, -46.63, accuracy, null)

    private suspend fun enqueue(
        shiftId: String,
        count: Int,
        from: Long = 0,
    ) = repeat(count) { queue.enqueue(shiftId, fix(from + it * 5L)) }

    private fun enqueueResponses(vararg responses: MockResponse) = responses.forEach(server::enqueue)

    private fun pointsIn(request: RecordedRequest) =
        Json
            .parseToJsonElement(request.body!!.utf8())
            .jsonObject
            .getValue("points")
            .jsonArray

    @Test
    fun ignoresRepeatedPoints() =
        runTest {
            queue.enqueue("s1", fix(0))
            queue.enqueue("s1", fix(0))
            queue.enqueue("s2", fix(0))

            assertEquals(1, queue.pending("s1"))
            assertEquals(1, queue.pending("s2"))
        }

    @Test
    fun sendsInBatchesOfFiveHundredAndEmptiesQueue() =
        runTest {
            enqueue("s1", 1200)
            enqueueResponses(accepted, accepted, accepted)

            assertEquals(FlushResult.Done, queue.flush())

            val sizes = (1..3).map { pointsIn(server.takeRequest()).size }
            assertEquals(listOf(500, 500, 200), sizes)
            assertEquals(0, queue.pending("s1"))
        }

    @Test
    fun bodyFollowsContract() =
        runTest {
            queue.enqueue("s1", fix(0))
            queue.enqueue("s1", fix(5, accuracy = null))
            enqueueResponses(accepted)

            queue.flush()

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/shifts/s1/track-points", request.url.encodedPath)
            val points = pointsIn(request)
            val first = points[0].jsonObject
            assertEquals(setOf("recorded_at", "latitude", "longitude", "accuracy_m"), first.keys)
            assertEquals(start, Instant.parse(first.getValue("recorded_at").toString().trim('"')))
            assertEquals(setOf("recorded_at", "latitude", "longitude"), points[1].jsonObject.keys)
        }

    @Test
    fun serverFailureKeepsPointsAndAsksRetry() =
        runTest {
            enqueue("s1", 10)
            enqueueResponses(Fixtures.json(503, """{"detail":"indisponível"}"""))

            assertEquals(FlushResult.Retry, queue.flush())
            assertEquals(10, queue.pending("s1"))
        }

    @Test
    fun networkFailureKeepsPointsAndAsksRetry() =
        runTest {
            enqueue("s1", 3)
            server.close()

            assertEquals(FlushResult.Retry, queue.flush())
            assertEquals(3, queue.pending("s1"))
        }

    @Test
    fun finishedOrMissingShiftDiscardsBatchAndContinues() =
        runTest {
            enqueue("old", 3)
            enqueue("gone", 2, from = 100)
            enqueue("s1", 4, from = 200)
            enqueueResponses(
                Fixtures.response(409, "error_shift_finished"),
                Fixtures.response(404, "error_not_found"),
                accepted,
            )

            assertEquals(FlushResult.Done, queue.flush())

            val paths =
                (1..3).map {
                    server
                        .takeRequest()
                        .url.encodedPath
                        .split("/")[4]
                }
            assertEquals(listOf("old", "gone", "s1"), paths)
            assertEquals(0, queue.pending("old") + queue.pending("gone") + queue.pending("s1"))
        }

    @Test
    fun unauthorizedStopsWithoutDeleting() =
        runTest {
            enqueue("s1", 5)
            enqueueResponses(Fixtures.response(401, "error_unauthenticated"))

            assertEquals(FlushResult.Unauthorized, queue.flush())
            assertEquals(5, queue.pending("s1"))
        }

    @Test
    fun clearRemovesEverything() =
        runTest {
            enqueue("s1", 5)

            queue.clear()

            assertEquals(0, queue.pending("s1"))
        }
}
