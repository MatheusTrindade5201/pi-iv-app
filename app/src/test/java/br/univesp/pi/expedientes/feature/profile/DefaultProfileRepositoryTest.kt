package br.univesp.pi.expedientes.feature.profile

import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.NetworkModule
import br.univesp.pi.expedientes.core.network.api.FuelApi
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.network.api.UserApi
import br.univesp.pi.expedientes.core.network.api.VehicleApi
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList

class DefaultProfileRepositoryTest {
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private var routes: Map<String, MockResponse> = emptyMap()

    @Before
    fun setUp() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += request
                    val key = "${request.method} ${request.url.encodedPath.removePrefix("/api/v1/")}"
                    return routes[key] ?: Fixtures.json(404, """{"detail":"rota $key não mapeada"}""")
                }
            }
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun repository(): DefaultProfileRepository {
        val retrofit = NetworkModule.provideRetrofit(server.url("/api/v1/").toString(), OkHttpClient(), ApiJson)
        return DefaultProfileRepository(
            retrofit.create(UserApi::class.java),
            retrofit.create(VehicleApi::class.java),
            retrofit.create(FuelApi::class.java),
        )
    }

    private fun bodyOf(method: String): JsonObject =
        Json.parseToJsonElement(requireNotNull(requests.single { it.method == method }.body).utf8()).jsonObject

    @Test
    fun loadCombinesUserFirstVehicleAndPrices() =
        runTest {
            routes =
                mapOf(
                    "GET users/me" to Fixtures.response(200, "users_me"),
                    "GET vehicles" to Fixtures.response(200, "vehicles"),
                    "GET fuel-prices/current" to Fixtures.response(200, "fuel_prices_current"),
                )

            val result = repository().load()

            val data = (result as ApiResult.Success).value
            assertEquals("John Doe", data.user.name)
            assertEquals("Honda CG 160", data.vehicle?.model)
            assertEquals(2, data.prices.size)
        }

    @Test
    fun loadFailsIfAnyCallFails() =
        runTest {
            routes =
                mapOf(
                    "GET users/me" to Fixtures.response(200, "users_me"),
                    "GET vehicles" to Fixtures.response(200, "vehicles"),
                    "GET fuel-prices/current" to Fixtures.json(500, """{"detail":"falhou"}"""),
                )

            assertEquals(ApiResult.HttpError(500, "falhou"), repository().load())
        }

    @Test
    fun createVehicleSendsAllFields() =
        runTest {
            routes = mapOf("POST vehicles" to Fixtures.response(201, "vehicle"))

            repository().saveVehicle(null, VehicleInput("Honda CG 160", FuelIds.GASOLINE, BigDecimal("32"), 160, 2022))

            assertEquals(
                Json.parseToJsonElement(
                    """{"model":"Honda CG 160","fuel_id":"${FuelIds.GASOLINE}","declared_kml":"32","displacement_cc":160,"year":2022}""",
                ),
                bodyOf("POST"),
            )
        }

    @Test
    fun updateVehicleSendsNullsToClearOptionalFields() =
        runTest {
            routes = mapOf("PATCH vehicles/v1" to Fixtures.response(200, "vehicle_update"))

            val result =
                repository().saveVehicle(
                    "v1",
                    VehicleInput("Fan", FuelIds.FLEX, BigDecimal("35.5"), null, null),
                )

            assertTrue(result is ApiResult.Success)
            assertEquals(
                Json.parseToJsonElement(
                    """{"model":"Fan","fuel_id":"${FuelIds.FLEX}","declared_kml":"35.5","displacement_cc":null,"year":null}""",
                ),
                bodyOf("PATCH"),
            )
        }

    @Test
    fun updatePricesSendsOnlyGivenFuels() =
        runTest {
            routes = mapOf("PUT fuel-prices" to Fixtures.response(200, "fuel_prices_update"))

            repository().updatePrices(mapOf(FuelIds.GASOLINE to BigDecimal("6.49")))

            assertEquals(
                Json.parseToJsonElement("""{"prices":[{"fuel_id":"${FuelIds.GASOLINE}","price_per_liter":"6.49"}]}"""),
                bodyOf("PUT"),
            )
        }

    @Test
    fun updateNameSendsName() =
        runTest {
            routes = mapOf("PATCH users/me" to Fixtures.response(200, "users_me_update"))

            repository().updateName("John Doe")

            assertEquals(Json.parseToJsonElement("""{"name":"John Doe"}"""), bodyOf("PATCH"))
        }
}
