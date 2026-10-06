package br.univesp.pi.expedientes.feature.auth

import br.univesp.pi.expedientes.core.network.ApiJson
import br.univesp.pi.expedientes.core.network.NetworkModule
import br.univesp.pi.expedientes.core.network.api.AuthApi
import br.univesp.pi.expedientes.core.session.FakeSessionStore
import br.univesp.pi.expedientes.core.session.SessionManager
import br.univesp.pi.expedientes.testing.Fixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAuthRepositoryTest {
    private val server = MockWebServer()
    private val store = FakeSessionStore()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun TestScope.repository(): DefaultAuthRepository {
        val api =
            NetworkModule
                .provideRetrofit(server.url("/api/v1/").toString(), OkHttpClient(), ApiJson)
                .create(AuthApi::class.java)
        return DefaultAuthRepository(api, SessionManager(store, backgroundScope))
    }

    @Test
    fun loginStartsSession() =
        runTest(UnconfinedTestDispatcher()) {
            server.enqueue(Fixtures.response(200, "login"))

            val result = repository().login("john@example.com", "senha-segura-123")

            assertEquals(AuthResult.Success, result)
            assertEquals("token.ficticio.de.teste", store.saved)
            val request = server.takeRequest()
            assertEquals("/api/v1/auth/login", request.url.encodedPath)
            assertEquals("""{"email":"john@example.com","password":"senha-segura-123"}""", request.body?.utf8())
        }

    @Test
    fun wrongPasswordKeepsLoggedOut() =
        runTest(UnconfinedTestDispatcher()) {
            server.enqueue(Fixtures.response(401, "error_unauthorized"))

            val result = repository().login("john@example.com", "errada-123")

            assertEquals(AuthResult.Failure(AuthFailure.Api("E-mail ou senha inválidos")), result)
            assertNull(store.saved)
        }

    @Test
    fun registerThenLogsIn() =
        runTest(UnconfinedTestDispatcher()) {
            server.enqueue(Fixtures.response(201, "register"))
            server.enqueue(Fixtures.response(200, "login"))

            val result = repository().register("John Doe", "john@example.com", "senha-segura-123")

            assertEquals(AuthResult.Success, result)
            assertEquals("token.ficticio.de.teste", store.saved)
            assertEquals("/api/v1/auth/register", server.takeRequest().url.encodedPath)
            assertEquals("/api/v1/auth/login", server.takeRequest().url.encodedPath)
        }

    @Test
    fun duplicatedEmailIsAnEmailFieldError() =
        runTest(UnconfinedTestDispatcher()) {
            server.enqueue(Fixtures.response(409, "error_conflict"))

            val result = repository().register("John Doe", "john@example.com", "senha-segura-123")

            assertEquals(
                AuthResult.Failure(AuthFailure.Api("E-mail já cadastrado", AuthField.Email)),
                result,
            )
            assertEquals(1, server.requestCount)
        }

    @Test
    fun serverDownIsNetworkFailure() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = repository()
            server.close()

            assertEquals(AuthResult.Failure(AuthFailure.Network), repository.login("john@example.com", "x"))
        }
}
