package br.univesp.pi.expedientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.univesp.pi.expedientes.core.connectivity.NetworkMonitor
import br.univesp.pi.expedientes.core.session.SessionExpiry
import br.univesp.pi.expedientes.core.session.SessionManager
import br.univesp.pi.expedientes.feature.tracking.TrackingController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        sessionManager: SessionManager,
        tracking: TrackingController,
        networkMonitor: NetworkMonitor,
        expiry: SessionExpiry,
    ) : ViewModel() {
        val isLoggedIn: StateFlow<Boolean?> = sessionManager.isLoggedIn
        val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

        init {
            viewModelScope.launch {
                sessionManager.isLoggedIn.collect { loggedIn ->
                    when (loggedIn) {
                        false -> {
                            tracking.stop()
                            tracking.clearPending()
                        }
                        true -> expiry.clear()
                        null -> Unit
                    }
                }
            }
        }
    }
