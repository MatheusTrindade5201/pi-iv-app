package br.univesp.pi.expedientes.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map

class FakeSessionStore(
    initial: String? = null,
    loaded: Boolean = true,
) : SessionStore {
    private val state = MutableStateFlow(if (loaded) Value(initial) else null)

    override val token = state.filterNot { it == null }.map { it?.token }

    val saved: String? get() = state.value?.token

    fun finishLoading(token: String?) {
        state.value = Value(token)
    }

    override suspend fun save(token: String) {
        state.value = Value(token)
    }

    override suspend fun clear() {
        state.value = Value(null)
    }

    private data class Value(
        val token: String?,
    )
}
