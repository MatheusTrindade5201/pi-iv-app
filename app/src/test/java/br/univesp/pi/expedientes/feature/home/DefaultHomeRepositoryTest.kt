package br.univesp.pi.expedientes.feature.home

import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.NetworkModule
import br.univesp.pi.expedientes.core.network.api.AnalyticsApi
import br.univesp.pi.expedientes.core.network.api.EarningApi
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.ShiftApi
import br.univesp.pi.expedientes.core.network.api.ShiftStatus
import br.univesp.pi.expedientes.core.network.api.UserApi
import br.univesp.pi.expedientes.core.network.api.VehicleApi
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList

class DefaultHomeRepositoryTest {
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private var routes: Map<String, MockResponse> = emptyMap()

    private val noShift = Fixtures.json(404, """{"detail":"Nenhum expediente em andamento"}""")

    @Before
    fun setUp() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += request
                    val key = "${request.method} ${request.url.encodedPath.removePrefix("/api/v1/")}"
                    return routes[key] ?: Fixtures.json(500, """{"detail":"rota $key não mapeada"}""")
                }
            }
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun repository(): DefaultHomeRepository {
        val retrofit = NetworkModule.provideRetrofit(server.url("/api/v1/").toString(), OkHttpClient(), ApiJson)
        return DefaultHomeRepository(
            retrofit.create(UserApi::class.java),
            retrofit.create(ShiftApi::class.java),
            retrofit.create(AnalyticsApi::class.java),
            retrofit.create(VehicleApi::class.java),
            retrofit.create(EarningApi::class.java),
        )
    }

    @Test
    fun withoutShiftInProgressReturnsLastDay() =
        runTest {
            routes =
                mapOf(
                    "GET users/me" to Fixtures.response(200, "users_me"),
                    "GET shifts/current" to noShift,
                    "GET analytics/daily" to Fixtures.response(200, "analytics_daily"),
                )

            val data = (repository().load() as ApiResult.Success).value

            assertEquals("John Doe", data.userName)
            assertNull(data.currentShift)
            assertEquals(1, data.lastDay?.shiftsCount)
        }

    @Test
    fun withShiftInProgress() =
        runTest {
            routes =
                mapOf(
                    "GET users/me" to Fixtures.response(200, "users_me"),
                    "GET shifts/current" to Fixtures.response(200, "shift_current"),
                    "GET analytics/daily" to Fixtures.json(200, "[]"),
                )

            val data = (repository().load() as ApiResult.Success).value

            assertEquals(ShiftStatus.InProgress, data.currentShift?.status)
            assertNull(data.lastDay)
        }

    @Test
    fun otherErrorsOnCurrentShiftFailTheLoad() =
        runTest {
            routes =
                mapOf(
                    "GET users/me" to Fixtures.response(200, "users_me"),
                    "GET shifts/current" to Fixtures.json(500, """{"detail":"falhou"}"""),
                    "GET analytics/daily" to Fixtures.json(200, "[]"),
                )

            assertEquals(ApiResult.HttpError(500, "falhou"), repository().load())
        }

    @Test
    fun firstVehicleOrNull() =
        runTest {
            routes = mapOf("GET vehicles" to Fixtures.response(200, "vehicles"))
            assertEquals("Honda CG 160", (repository().firstVehicle() as ApiResult.Success).value?.model)

            routes = mapOf("GET vehicles" to Fixtures.json(200, "[]"))
            assertEquals(ApiResult.Success(null), repository().firstVehicle())
        }

    @Test
    fun startShiftSendsFuelOnlyWhenGiven() =
        runTest {
            routes = mapOf("POST shifts" to Fixtures.response(201, "shift_started"))

            repository().startShift("v1", null)
            repository().startShift("v2", FuelIds.ETHANOL)

            val bodies = requests.filter { it.method == "POST" }.map { Json.parseToJsonElement(it.body!!.utf8()) }
            assertEquals(
                listOf(
                    Json.parseToJsonElement("""{"vehicle_id":"v1"}"""),
                    Json.parseToJsonElement("""{"vehicle_id":"v2","fuel_id":"${FuelIds.ETHANOL}"}"""),
                ),
                bodies,
            )
        }

    @Test
    fun currentShiftIsNullWhenNoneInProgress() =
        runTest {
            routes = mapOf("GET shifts/current" to noShift)
            assertEquals(ApiResult.Success(null), repository().currentShift())

            routes = mapOf("GET shifts/current" to Fixtures.response(200, "shift_current"))
            assertEquals(ShiftStatus.InProgress, (repository().currentShift() as ApiResult.Success).value?.status)
        }

    @Test
    fun lastEarningIsTheMostRecent() =
        runTest {
            routes = mapOf("GET shifts/s1/earnings" to Fixtures.response(200, "earnings"))
            assertEquals(
                "9e3d3093-050e-4a1d-b2b9-7ba67cae2236",
                (repository().lastEarning("s1") as ApiResult.Success).value?.id,
            )

            routes = mapOf("GET shifts/s1/earnings" to Fixtures.json(200, "[]"))
            assertEquals(ApiResult.Success(null), repository().lastEarning("s1"))
        }

    @Test
    fun addAndOverwriteSendAmountAsString() =
        runTest {
            routes =
                mapOf(
                    "POST shifts/s1/earnings" to Fixtures.response(201, "earning"),
                    "PATCH shifts/s1/earnings/e1" to Fixtures.response(200, "earning_update"),
                )

            repository().addEarning("s1", BigDecimal("20.00"))
            repository().overwriteEarning("s1", "e1", BigDecimal("8"))

            val bodies = requests.map { it.method to Json.parseToJsonElement(it.body!!.utf8()) }
            assertEquals(
                listOf(
                    "POST" to Json.parseToJsonElement("""{"amount":"20.00"}"""),
                    "PATCH" to Json.parseToJsonElement("""{"amount":"8"}"""),
                ),
                bodies,
            )
        }

    @Test
    fun finishShift() =
        runTest {
            routes = mapOf("POST shifts/s1/finish" to Fixtures.response(200, "shift_finished"))

            assertEquals(ShiftStatus.Finished, (repository().finishShift("s1") as ApiResult.Success).value.status)
        }
}
