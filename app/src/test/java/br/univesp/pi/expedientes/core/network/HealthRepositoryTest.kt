package br.univesp.pi.expedientes.core.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HealthRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: DefaultHealthRepository

    @Before
    fun setUp() {
        server.start()
        val retrofit =
            NetworkModule.provideRetrofit(
                baseUrl = server.url("/api/v1/").toString(),
                client = OkHttpClient(),
                json = NetworkModule.provideJson(),
            )
        repository = DefaultHealthRepository(NetworkModule.provideHealthApi(retrofit))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `healthy api is connected`() =
        runTest {
            server.enqueue(json(200, """{"status":"ok","database":"ok"}"""))

            assertEquals(ApiStatus.Connected, repository.check())
            assertEquals("/api/v1/health", server.takeRequest().url.encodedPath)
        }

    @Test
    fun `database down is unavailable`() =
        runTest {
            server.enqueue(json(503, """{"status":"error","database":"unavailable"}"""))

            assertEquals(ApiStatus.Unavailable, repository.check())
        }

    @Test
    fun `unexpected body is unavailable`() =
        runTest {
            server.enqueue(json(200, """{"unexpected":true}"""))

            assertEquals(ApiStatus.Unavailable, repository.check())
        }

    @Test
    fun `unreachable server is unavailable`() =
        runTest {
            server.close()

            assertEquals(ApiStatus.Unavailable, repository.check())
        }

    private fun json(
        code: Int,
        body: String,
    ): MockResponse =
        MockResponse
            .Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
}
