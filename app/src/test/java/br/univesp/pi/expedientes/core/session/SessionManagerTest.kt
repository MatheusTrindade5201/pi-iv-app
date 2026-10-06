package br.univesp.pi.expedientes.core.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {
    @Test
    fun loadingThenLoggedOut() =
        runTest(UnconfinedTestDispatcher()) {
            val store = FakeSessionStore(loaded = false)
            val manager = SessionManager(store, backgroundScope)

            assertNull(manager.isLoggedIn.value)

            store.finishLoading(null)

            assertEquals(false, manager.isLoggedIn.value)
            assertNull(manager.currentToken)
        }

    @Test
    fun restoresSavedToken() =
        runTest(UnconfinedTestDispatcher()) {
            val manager = SessionManager(FakeSessionStore("abc"), backgroundScope)

            assertEquals(true, manager.isLoggedIn.value)
            assertEquals("abc", manager.currentToken)
        }

    @Test
    fun startSessionSavesAndExposesTokenImmediately() =
        runTest(UnconfinedTestDispatcher()) {
            val store = FakeSessionStore()
            val manager = SessionManager(store, backgroundScope)

            manager.startSession("novo-token")

            assertEquals("novo-token", manager.currentToken)
            assertEquals("novo-token", store.saved)
            assertEquals(true, manager.isLoggedIn.value)
        }

    @Test
    fun logoutClearsEverything() =
        runTest(UnconfinedTestDispatcher()) {
            val store = FakeSessionStore("abc")
            val manager = SessionManager(store, backgroundScope)

            manager.logout()
            advanceUntilIdle()

            assertNull(manager.currentToken)
            assertNull(store.saved)
            assertEquals(false, manager.isLoggedIn.value)
        }
}
