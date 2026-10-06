package br.univesp.pi.expedientes.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.univesp.pi.expedientes.core.network.ApiBaseUrl
import br.univesp.pi.expedientes.core.network.ApiStatus
import br.univesp.pi.expedientes.core.network.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val apiBaseUrl: String,
    val apiStatus: ApiStatus? = null,
) {
    val isChecking: Boolean get() = apiStatus == null
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val healthRepository: HealthRepository,
        @ApiBaseUrl apiBaseUrl: String,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState(apiBaseUrl = apiBaseUrl))
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            checkApi()
        }

        fun checkApi() {
            _uiState.update { it.copy(apiStatus = null) }
            viewModelScope.launch {
                val status = healthRepository.check()
                _uiState.update { it.copy(apiStatus = status) }
            }
        }
    }
