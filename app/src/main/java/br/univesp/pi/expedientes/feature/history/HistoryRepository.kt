package br.univesp.pi.expedientes.feature.history

import br.univesp.pi.expedientes.core.network.ApiResult
import br.univesp.pi.expedientes.core.network.api.AnalyticsApi
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.PeriodSummary
import br.univesp.pi.expedientes.core.network.api.ShiftApi
import br.univesp.pi.expedientes.core.network.apiCall
import br.univesp.pi.expedientes.core.network.asFailure
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HistoryData(
    val days: List<DailyTotals>,
    val summary: PeriodSummary,
    val shiftInProgress: Boolean,
)

interface HistoryRepository {
    suspend fun load(
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
    ): ApiResult<HistoryData>
}

class DefaultHistoryRepository
    @Inject
    constructor(
        private val analyticsApi: AnalyticsApi,
        private val shiftApi: ShiftApi,
    ) : HistoryRepository {
        override suspend fun load(
            from: LocalDate,
            to: LocalDate,
            zone: ZoneId,
        ): ApiResult<HistoryData> =
            coroutineScope {
                val daysCall = async { apiCall { analyticsApi.daily(from, to, zone.id) } }
                val summaryCall = async { apiCall { analyticsApi.summary(from, to, zone.id) } }
                val currentCall = async { apiCall { shiftApi.current() } }
                val days = daysCall.await()
                val summary = summaryCall.await()
                val current = currentCall.await()
                val noShift = current is ApiResult.HttpError && current.code == 404
                when {
                    days !is ApiResult.Success -> days.asFailure()
                    summary !is ApiResult.Success -> summary.asFailure()
                    current !is ApiResult.Success && !noShift -> current.asFailure()
                    else -> ApiResult.Success(HistoryData(days.value, summary.value, shiftInProgress = !noShift))
                }
            }
    }

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryModule {
    @Binds
    abstract fun bindHistoryRepository(impl: DefaultHistoryRepository): HistoryRepository
}
