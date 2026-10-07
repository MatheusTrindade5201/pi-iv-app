package br.univesp.pi.expedientes.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionExpiry
    @Inject
    constructor() {
        private val _expired = MutableStateFlow(false)
        val expired: StateFlow<Boolean> = _expired.asStateFlow()

        fun expire() {
            _expired.value = true
        }

        fun clear() {
            _expired.value = false
        }
    }
