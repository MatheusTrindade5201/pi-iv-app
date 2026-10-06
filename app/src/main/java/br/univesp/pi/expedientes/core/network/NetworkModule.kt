package br.univesp.pi.expedientes.core.network

import br.univesp.pi.expedientes.BuildConfig
import br.univesp.pi.expedientes.core.network.api.AnalyticsApi
import br.univesp.pi.expedientes.core.network.api.AuthApi
import br.univesp.pi.expedientes.core.network.api.EarningApi
import br.univesp.pi.expedientes.core.network.api.FuelApi
import br.univesp.pi.expedientes.core.network.api.HealthApi
import br.univesp.pi.expedientes.core.network.api.ShiftApi
import br.univesp.pi.expedientes.core.network.api.TrackPointApi
import br.univesp.pi.expedientes.core.network.api.UserApi
import br.univesp.pi.expedientes.core.network.api.VehicleApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiBaseUrl

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @ApiBaseUrl
    fun provideApiBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideJson(): Json = ApiJson

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }.build()

    @Provides
    @Singleton
    fun provideRetrofit(
        @ApiBaseUrl baseUrl: String,
        client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideHealthApi(retrofit: Retrofit): HealthApi = retrofit.create(HealthApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideFuelApi(retrofit: Retrofit): FuelApi = retrofit.create(FuelApi::class.java)

    @Provides
    @Singleton
    fun provideVehicleApi(retrofit: Retrofit): VehicleApi = retrofit.create(VehicleApi::class.java)

    @Provides
    @Singleton
    fun provideShiftApi(retrofit: Retrofit): ShiftApi = retrofit.create(ShiftApi::class.java)

    @Provides
    @Singleton
    fun provideEarningApi(retrofit: Retrofit): EarningApi = retrofit.create(EarningApi::class.java)

    @Provides
    @Singleton
    fun provideTrackPointApi(retrofit: Retrofit): TrackPointApi = retrofit.create(TrackPointApi::class.java)

    @Provides
    @Singleton
    fun provideAnalyticsApi(retrofit: Retrofit): AnalyticsApi = retrofit.create(AnalyticsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindHealthRepository(impl: DefaultHealthRepository): HealthRepository
}
