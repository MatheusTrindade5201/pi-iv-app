package br.univesp.pi.expedientes.core.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreSessionStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun savesReadsAndClearsToken() =
        runTest {
            val store =
                DataStoreSessionStore(
                    PreferenceDataStoreFactory.create(scope = backgroundScope) {
                        File(folder.root, "session.preferences_pb")
                    },
                )

            assertNull(store.token.first())

            store.save("token-123")
            assertEquals("token-123", store.token.first())

            store.clear()
            assertNull(store.token.first())
        }
}
