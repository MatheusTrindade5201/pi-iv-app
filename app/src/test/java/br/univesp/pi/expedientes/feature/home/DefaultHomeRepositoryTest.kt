package br.univesp.pi.expedientes.feature.home

import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.NetworkModule
import br.univesp.pi.expedientes.core.network.api.AnalyticsApi
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
}
