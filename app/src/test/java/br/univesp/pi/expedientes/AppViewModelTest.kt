package br.univesp.pi.expedientes

import br.univesp.pi.expedientes.core.session.FakeSessionStore
import br.univesp.pi.expedientes.core.session.SessionManager
import br.univesp.pi.expedientes.testing.FakeTrackingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun logoutStopsTrackingAndDropsPendingPoints() =
        runTest(mainDispatcherRule.dispatcher) {
            val sessionScope = CoroutineScope(mainDispatcherRule.dispatcher + Job())
            val sessionManager = SessionManager(FakeSessionStore("token"), sessionScope)
            val tracking = FakeTrackingController()
            AppViewModel(sessionManager, tracking)
            advanceUntilIdle()
            assertEquals(emptyList<String>(), tracking.calls)

            sessionManager.logout()
            advanceUntilIdle()

            assertEquals(listOf("stop", "clear"), tracking.calls)
            sessionScope.cancel()
        }
}
