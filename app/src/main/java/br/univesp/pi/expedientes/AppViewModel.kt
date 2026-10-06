package br.univesp.pi.expedientes

import androidx.lifecycle.ViewModel
import br.univesp.pi.expedientes.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        sessionManager: SessionManager,
    ) : ViewModel() {
        val isLoggedIn: StateFlow<Boolean?> = sessionManager.isLoggedIn
    }
