package br.univesp.pi.expedientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.univesp.pi.expedientes.core.session.SessionManager
import br.univesp.pi.expedientes.feature.tracking.TrackingController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        sessionManager: SessionManager,
        tracking: TrackingController,
    ) : ViewModel() {
        val isLoggedIn: StateFlow<Boolean?> = sessionManager.isLoggedIn

        init {
            viewModelScope.launch {
                sessionManager.isLoggedIn.filter { it == false }.collect {
                    tracking.stop()
                    tracking.clearPending()
                }
            }
        }
    }
