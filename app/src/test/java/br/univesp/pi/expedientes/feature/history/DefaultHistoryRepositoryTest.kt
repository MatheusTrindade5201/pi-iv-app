package br.univesp.pi.expedientes.feature.history

import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.NetworkModule
import br.univesp.pi.expedientes.core.network.api.AnalyticsApi
import br.univesp.pi.expedientes.core.network.api.ShiftApi
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList

class DefaultHistoryRepositoryTest {
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private var current: MockResponse = Fixtures.json(404, """{"detail":"Nenhum expediente em andamento"}""")
    private var summary: MockResponse = Fixtures.response(200, "analytics_summary")

    @Before
    fun setUp() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += request
                    return when (request.url.encodedPath.removePrefix("/api/v1/")) {
                        "analytics/daily" -> Fixtures.response(200, "analytics_daily")
                        "analytics/summary" -> summary
                        "shifts/current" -> current
                        else -> Fixtures.json(500, "{}")
                    }
                }
            }
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private suspend fun load(): ApiResult<HistoryData> {
        val retrofit = NetworkModule.provideRetrofit(server.url("/api/v1/").toString(), OkHttpClient(), ApiJson)
        return DefaultHistoryRepository(
            retrofit.create(AnalyticsApi::class.java),
            retrofit.create(ShiftApi::class.java),
        ).load(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 10, 6), ZoneId.of("America/Sao_Paulo"))
    }

    @Test
    fun loadsDaysAndSummaryForThePeriod() =
        runTest {
            val data = (load() as ApiResult.Success).value

            assertEquals(LocalDate.of(2026, 10, 6), data.days.single().date)
            assertEquals(BigDecimal("18.64"), data.summary.avgProfitPerDay)
            assertEquals(false, data.shiftInProgress)
            val analytics = requests.filter { it.url.encodedPath.contains("analytics") }
            assertEquals(2, analytics.size)
            analytics.forEach {
                assertEquals("2026-09-07", it.url.queryParameter("from"))
                assertEquals("2026-10-06", it.url.queryParameter("to"))
                assertEquals("America/Sao_Paulo", it.url.queryParameter("tz"))
            }
        }

    @Test
    fun knowsWhenAShiftIsRunning() =
        runTest {
            current = Fixtures.response(200, "shift_current")

            assertEquals(true, (load() as ApiResult.Success).value.shiftInProgress)
        }

    @Test
    fun anyFailureFailsTheLoad() =
        runTest {
            summary = Fixtures.response(422, "error_validation")
            assertEquals(422, (load() as ApiResult.HttpError).code)

            summary = Fixtures.response(200, "analytics_summary")
            current = Fixtures.json(500, """{"detail":"falhou"}""")
            assertEquals(ApiResult.HttpError(500, "falhou"), load())
        }
}
