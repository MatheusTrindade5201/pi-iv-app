package br.univesp.pi.expedientes.core.network

import br.univesp.pi.expedientes.core.network.api.AuthApi
import br.univesp.pi.expedientes.core.network.api.LoginRequest
import br.univesp.pi.expedientes.core.network.api.UserApi
import br.univesp.pi.expedientes.core.session.FakeSessionStore
import br.univesp.pi.expedientes.core.session.SessionExpiry
import br.univesp.pi.expedientes.core.session.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

@OptIn(ExperimentalCoroutinesApi::class)
class AuthInterceptorsTest {
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private val expiry = SessionExpiry()

    private fun retrofit(sessionManager: SessionManager): Retrofit =
        NetworkModule.provideRetrofit(
            baseUrl = server.url("/api/v1/").toString(),
            client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(AuthInterceptor(sessionManager))
                    .addInterceptor(UnauthorizedInterceptor(sessionManager, expiry))
                    .build(),
            json = ApiJson,
        )

    private fun TestScope.session(token: String?): SessionManager =
        SessionManager(FakeSessionStore(token), backgroundScope)

    private fun json(
        code: Int,
        body: String,
    ) = MockResponse
        .Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private val user = """{"id":"1","name":"John","email":"j@x.com","created_at":"2026-10-06T10:00:00Z"}"""

    @Test
    fun sendsBearerTokenWhenLoggedIn() =
        runTest(UnconfinedTestDispatcher()) {
            server.enqueue(json(200, user))

            retrofit(session("abc")).create(UserApi::class.java).me()

            assertEquals("Bearer abc", server.takeRequest().headers["Authorization"])
        }

    @Test
    fun noHeaderWithoutSession() =
        runTest(UnconfinedTestDispatcher()) {
            server.enqueue(json(401, """{"detail":"Não autenticado"}"""))

            apiCall { retrofit(session(null)).create(UserApi::class.java).me() }

            assertNull(server.takeRequest().headers["Authorization"])
        }

    @Test
    fun loginNeverSendsToken() =
        runTest(UnconfinedTestDispatcher()) {
            server.enqueue(json(200, """{"access_token":"t","token_type":"bearer"}"""))

            retrofit(session("antigo")).create(AuthApi::class.java).login(LoginRequest("a@b.com", "x"))

            assertNull(server.takeRequest().headers["Authorization"])
        }

    @Test
    fun unauthorizedOnProtectedRouteLogsOut() =
        runTest(UnconfinedTestDispatcher()) {
            val sessionManager = session("expirado")
            server.enqueue(json(401, """{"detail":"Não autenticado"}"""))

            val result = apiCall { retrofit(sessionManager).create(UserApi::class.java).me() }

            assertEquals(ApiResult.HttpError(401, "Não autenticado"), result)
            assertEquals(false, sessionManager.isLoggedIn.value)
            assertEquals(true, expiry.expired.value)
        }

    @Test
    fun wrongPasswordDoesNotLogOut() =
        runTest(UnconfinedTestDispatcher()) {
            val sessionManager = session("valido")
            server.enqueue(json(401, """{"detail":"E-mail ou senha inválidos"}"""))

            apiCall { retrofit(sessionManager).create(AuthApi::class.java).login(LoginRequest("a@b.com", "x")) }

            assertEquals(true, sessionManager.isLoggedIn.value)
            assertEquals(false, expiry.expired.value)
        }
}
