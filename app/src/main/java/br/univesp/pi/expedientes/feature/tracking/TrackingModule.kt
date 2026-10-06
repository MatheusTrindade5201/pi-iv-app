package br.univesp.pi.expedientes.feature.tracking

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointDao
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointQueue
import br.univesp.pi.expedientes.feature.tracking.data.TrackPointSink
import br.univesp.pi.expedientes.feature.tracking.data.TrackingDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackingProvidersModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): TrackingDatabase = Room.databaseBuilder(context, TrackingDatabase::class.java, "tracking.db").build()

    @Provides
    fun provideTrackPointDao(database: TrackingDatabase): TrackPointDao = database.trackPointDao()

    @Provides
    @Singleton
    @TrackingPreferences
    fun provideTrackingPreferences(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("tracking") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingBindingsModule {
    @Binds
    abstract fun bindSink(impl: TrackPointQueue): TrackPointSink

    @Binds
    abstract fun bindLocationSource(impl: FusedLocationSource): LocationSource

    @Binds
    abstract fun bindScheduler(impl: WorkUploadScheduler): UploadScheduler

    @Binds
    abstract fun bindController(impl: AndroidTrackingController): TrackingController
}
