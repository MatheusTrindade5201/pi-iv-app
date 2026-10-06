package br.univesp.pi.expedientes.feature.home

import app.cash.turbine.test
import br.univesp.pi.expedientes.MainDispatcherRule
import br.univesp.pi.expedientes.core.network.ApiStatus
import br.univesp.pi.expedientes.core.network.HealthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeHealthRepository(
        var next: ApiStatus,
    ) : HealthRepository {
        var calls = 0

        override suspend fun check(): ApiStatus {
            calls++
            return next
        }
    }

    @Test
    fun `starts checking and becomes connected`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = HomeViewModel(FakeHealthRepository(ApiStatus.Connected), BASE_URL)

            viewModel.uiState.test {
                val initial = awaitItem()
                assertTrue(initial.isChecking)
                assertEquals(BASE_URL, initial.apiBaseUrl)
                assertEquals(ApiStatus.Connected, awaitItem().apiStatus)
            }
        }

    @Test
    fun `retry checks again after unavailable`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakeHealthRepository(ApiStatus.Unavailable)
            val viewModel = HomeViewModel(repository, BASE_URL)

            viewModel.uiState.test {
                assertTrue(awaitItem().isChecking)
                assertEquals(ApiStatus.Unavailable, awaitItem().apiStatus)

                repository.next = ApiStatus.Connected
                viewModel.checkApi()

                assertTrue(awaitItem().isChecking)
                assertEquals(ApiStatus.Connected, awaitItem().apiStatus)
            }
            assertEquals(2, repository.calls)
        }

    private companion object {
        const val BASE_URL = "http://10.0.2.2:8000/api/v1/"
    }
}
