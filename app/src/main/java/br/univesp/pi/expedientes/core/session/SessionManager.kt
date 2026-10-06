package br.univesp.pi.expedientes.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Singleton
class SessionManager
    @Inject
    constructor(
        private val store: SessionStore,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val token = MutableStateFlow<String?>(null)
        private val loaded = MutableStateFlow(false)

        val isLoggedIn: StateFlow<Boolean?> =
            combine(loaded, token) { isLoaded, current -> if (isLoaded) current != null else null }
                .stateIn(scope, SharingStarted.Eagerly, null)

        val currentToken: String? get() = token.value

        init {
            scope.launch {
                store.token.collect {
                    token.value = it
                    loaded.value = true
                }
            }
        }

        suspend fun startSession(accessToken: String) {
            token.value = accessToken
            store.save(accessToken)
        }

        fun logout() {
            token.value = null
            scope.launch { store.clear() }
        }
    }
