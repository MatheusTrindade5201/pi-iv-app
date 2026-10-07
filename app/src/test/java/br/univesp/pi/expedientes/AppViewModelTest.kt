package br.univesp.pi.expedientes

import br.univesp.pi.expedientes.core.connectivity.NetworkMonitor
import br.univesp.pi.expedientes.core.session.FakeSessionStore
import br.univesp.pi.expedientes.core.session.SessionExpiry
import br.univesp.pi.expedientes.core.session.SessionManager
import br.univesp.pi.expedientes.testing.FakeTrackingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sessionScope = CoroutineScope(mainDispatcherRule.dispatcher + Job())
    private val tracking = FakeTrackingController()
    private val expiry = SessionExpiry()
    private val network =
        object : NetworkMonitor {
            override val isOnline = MutableStateFlow(true)
        }

    @After
    fun tearDown() {
        sessionScope.cancel()
    }

    private fun TestScope.viewModel(token: String?): Pair<SessionManager, AppViewModel> {
        val sessionManager = SessionManager(FakeSessionStore(token), sessionScope)
        val viewModel = AppViewModel(sessionManager, tracking, network, expiry)
        advanceUntilIdle()
        return sessionManager to viewModel
    }

    @Test
    fun logoutStopsTrackingAndDropsPendingPoints() =
        runTest(mainDispatcherRule.dispatcher) {
            val (sessionManager, _) = viewModel("token")
            assertEquals(emptyList<String>(), tracking.calls)

            sessionManager.logout()
            advanceUntilIdle()

            assertEquals(listOf("stop", "clear"), tracking.calls)
        }

    @Test
    fun loggingInClearsExpiredNotice() =
        runTest(mainDispatcherRule.dispatcher) {
            expiry.expire()
            val (sessionManager, _) = viewModel(null)
            assertEquals(true, expiry.expired.value)

            sessionManager.startSession("novo")
            advanceUntilIdle()

            assertEquals(false, expiry.expired.value)
        }

    @Test
    fun exposesConnectivity() =
        runTest(mainDispatcherRule.dispatcher) {
            val (_, viewModel) = viewModel("token")

            network.isOnline.value = false

            assertEquals(false, viewModel.isOnline.value)
        }
}
