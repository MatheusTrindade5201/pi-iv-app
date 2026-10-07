package br.univesp.pi.expedientes.feature.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShiftStartRequests
    @Inject
    constructor() {
        private val _pending = MutableStateFlow(false)
        val pending: StateFlow<Boolean> = _pending.asStateFlow()

        fun request() {
            _pending.value = true
        }

        fun consume(): Boolean = _pending.getAndUpdate { false }
    }
