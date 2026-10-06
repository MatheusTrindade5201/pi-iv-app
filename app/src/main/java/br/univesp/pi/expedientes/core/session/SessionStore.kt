package br.univesp.pi.expedientes.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SessionStore {
    val token: Flow<String?>

    suspend fun save(token: String)

    suspend fun clear()
}

class DataStoreSessionStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SessionStore {
        override val token: Flow<String?> = dataStore.data.map { it[TOKEN] }

        override suspend fun save(token: String) {
            dataStore.edit { it[TOKEN] = token }
        }

        override suspend fun clear() {
            dataStore.edit { it.remove(TOKEN) }
        }

        private companion object {
            val TOKEN = stringPreferencesKey("access_token")
        }
    }
