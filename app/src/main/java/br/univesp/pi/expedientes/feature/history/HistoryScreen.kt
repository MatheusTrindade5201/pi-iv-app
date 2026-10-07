package br.univesp.pi.expedientes.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.format.formatBrl
import br.univesp.pi.expedientes.core.format.perHour
import br.univesp.pi.expedientes.core.format.perKm
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.PeriodSummary
import br.univesp.pi.expedientes.core.ui.components.ChoiceChips
import br.univesp.pi.expedientes.core.ui.components.DetailList
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.components.ScreenTitle
import br.univesp.pi.expedientes.core.ui.components.SectionCard
import br.univesp.pi.expedientes.core.ui.components.text
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.feature.home.ProfitText
import br.univesp.pi.expedientes.feature.home.ShiftDetails
import br.univesp.pi.expedientes.feature.home.dayLabel
import java.time.LocalDate

@Composable
fun HistoryEntry(
    onOpenHome: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.reload() }
    HistoryScreen(state = state, actions = viewModel, onOpenHome = onOpenHome)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    actions: HistoryActions,
    onOpenHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenTitle(text = stringResource(R.string.history_title), modifier = Modifier.padding(vertical = 16.dp))
        ChoiceChips(
            options =
                HISTORY_PERIODS.map {
                    it.toString() to
                        pluralStringResource(
                            R.plurals.history_period_days,
                            it,
                            it,
                        )
                },
            selected = state.periodDays.toString(),
            onSelect = { actions.selectPeriod(it.toInt()) },
            compact = true,
        )
        Box(modifier = Modifier.weight(1f).padding(top = 16.dp)) {
            when (val content = state.content) {
                HistoryContent.Loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                is HistoryContent.Failed -> FailedHistory(content.failure, actions::retry)
                is HistoryContent.Loaded ->
                    PullToRefreshBox(
                        isRefreshing = state.refreshing,
                        onRefresh = actions::pullToRefresh,
                        modifier = Modifier.fillMaxSize().testTag("history_refresh"),
                    ) {
                        HistoryList(state, content.data, actions)
                    }
            }
        }
        val inProgress = (state.content as? HistoryContent.Loaded)?.data?.shiftInProgress == true
        PrimaryButton(
            text = stringResource(if (inProgress) R.string.history_view_shift else R.string.home_start_shift),
            onClick = {
                if (!inProgress) actions.startShift()
                onOpenHome()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_play), contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("history_start"),
        )
    }
}

@Composable
private fun FailedHistory(
    failure: RequestFailure,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = failure.text(), color = MaterialTheme.colorScheme.error)
        PrimaryButton(text = stringResource(R.string.home_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun HistoryList(
    state: HistoryUiState,
    data: HistoryData,
    actions: HistoryActions,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("history_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.refreshFailure?.let { failure ->
            item {
                Text(
                    text = stringResource(R.string.history_refresh_failed, failure.text()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (data.days.isEmpty()) {
            item {
                Text(
                    text = pluralStringResource(R.plurals.history_empty, state.periodDays, state.periodDays),
                    color = ExpedientesTheme.colors.textSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("history_empty"),
                )
            }
            return@LazyColumn
        }
        item { SummaryCard(data.summary) }
        items(data.days, key = { it.date.toString() }) { day ->
            DayCard(day, state.today, expanded = day.date in state.expanded, onToggle = { actions.toggleDay(day.date) })
        }
    }
}

@Composable
private fun SummaryCard(summary: PeriodSummary) {
    SectionCard(modifier = Modifier.testTag("history_summary")) {
        Text(text = stringResource(R.string.history_summary), style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.history_profit),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            ProfitText(summary.profit, fontSize = 28)
        }
        ShiftDetails(
            earnings = summary.earningsTotal,
            fuelCost = summary.fuelCost,
            km = summary.distanceKm,
            minutes = summary.durationMinutes,
            hourly = summary.profitPerHour,
            perKmValue = summary.profitPerKm,
        )
        DetailList(
            items =
                listOf(
                    stringResource(R.string.history_days_worked) to summary.daysWorked.toString(),
                    stringResource(R.string.history_avg_day) to
                        (summary.avgProfitPerDay?.let(::formatBrl) ?: stringResource(R.string.profile_no_price)),
                ),
        )
    }
}

@Composable
private fun DayCard(
    day: DailyTotals,
    today: LocalDate,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    SectionCard(
        modifier =
            Modifier
                .clickable(role = Role.Button, onClick = onToggle)
                .testTag("day_${day.date}"),
    ) {
        Text(text = dayLabel(day.date, today), style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_day_profit),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            ProfitText(day.profit, fontSize = 28)
        }
        if (expanded) {
            ShiftDetails(
                earnings = day.earningsTotal,
                fuelCost = day.fuelCost,
                km = day.distanceKm,
                minutes = day.durationMinutes,
                hourly = perHour(day.profit, day.durationMinutes),
                perKmValue = perKm(day.profit, day.distanceKm),
            )
        }
    }
}
