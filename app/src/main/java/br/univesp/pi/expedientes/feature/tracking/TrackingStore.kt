package br.univesp.pi.expedientes.feature.tracking

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TrackingPreferences

@Singleton
class TrackingStore
    @Inject
    constructor(
        @TrackingPreferences private val dataStore: DataStore<Preferences>,
    ) {
        suspend fun activeShiftId(): String? = dataStore.data.map { it[ACTIVE_SHIFT] }.first()

        suspend fun setActiveShift(shiftId: String?) {
            dataStore.edit { prefs ->
                if (shiftId == null) prefs.remove(ACTIVE_SHIFT) else prefs[ACTIVE_SHIFT] = shiftId
            }
        }

        private companion object {
            val ACTIVE_SHIFT = stringPreferencesKey("active_shift_id")
        }
    }
