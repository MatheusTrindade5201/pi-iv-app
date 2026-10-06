package br.univesp.pi.expedientes.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.format.DayLabel
import br.univesp.pi.expedientes.core.format.formatAmount
import br.univesp.pi.expedientes.core.format.formatBrl
import br.univesp.pi.expedientes.core.format.formatDuration
import br.univesp.pi.expedientes.core.format.formatKm
import br.univesp.pi.expedientes.core.format.formatSignedBrl
import br.univesp.pi.expedientes.core.format.homeHeader
import br.univesp.pi.expedientes.core.format.perHour
import br.univesp.pi.expedientes.core.format.perKm
import br.univesp.pi.expedientes.core.format.relativeDay
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.DailyTotals
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.ui.components.DetailList
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.components.SectionCard
import br.univesp.pi.expedientes.core.ui.components.text
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun HomeEntry(
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    HomeScreen(state = state, actions = viewModel, onOpenProfile = onOpenProfile)
    StartShiftDialogs(state = state.startFlow, actions = viewModel, onOpenProfile = onOpenProfile)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val content = state.content) {
        HomeContent.Loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        is HomeContent.Failed -> FailedHome(content.failure, actions::retry, modifier)
        is HomeContent.Idle -> IdleHome(state, content, actions, onOpenProfile, modifier)
        is HomeContent.InProgress -> InProgressHome(content.shift, modifier)
    }
}

@Composable
private fun FailedHome(
    failure: RequestFailure,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = failure.text(), color = MaterialTheme.colorScheme.error)
        PrimaryButton(text = stringResource(R.string.home_retry), onClick = onRetry, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun IdleHome(
    state: HomeUiState,
    content: HomeContent.Idle,
    actions: HomeActions,
    onOpenProfile: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(text = homeHeader(state.today), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.home_greeting, content.userName.substringBefore(' ')),
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(8.dp))
            LastRecordCard(content.lastDay, state.today)
        }
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StartShiftError(state.startFlow, onOpenProfile)
            PrimaryButton(
                text = stringResource(R.string.home_start_shift),
                onClick = actions::startShift,
                loading = state.isStarting,
                leadingIcon = { Icon(painterResource(R.drawable.ic_play), contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("start_shift"),
            )
        }
    }
}

@Composable
private fun LastRecordCard(
    day: DailyTotals?,
    today: LocalDate,
) {
    SectionCard {
        if (day == null) {
            Text(
                text = stringResource(R.string.home_no_records),
                style = MaterialTheme.typography.bodyLarge,
                color = ExpedientesTheme.colors.textSecondary,
            )
            return@SectionCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_last_record),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(text = dayLabel(day.date, today), style = MaterialTheme.typography.bodyLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_day_profit),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            ProfitText(day.profit, fontSize = 28)
        }
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

@Composable
private fun InProgressHome(
    shift: ShiftResponse,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(ExpedientesTheme.colors.positive, CircleShape))
            Text(
                text = stringResource(R.string.home_in_progress),
                color = ExpedientesTheme.colors.positive,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            Text(
                text = formatDuration(shift.summary.durationMinutes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        SectionCard {
            Text(
                text = stringResource(R.string.home_partial_profit),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            ProfitText(
                value = shift.summary.profit,
                fontSize = 40,
                modifier = Modifier.align(Alignment.CenterHorizontally).testTag("partial_profit"),
            )
            ShiftDetails(
                earnings = shift.summary.earningsTotal,
                fuelCost = shift.summary.fuelCost,
                km = shift.summary.distanceKm,
                minutes = shift.summary.durationMinutes,
                hourly = shift.summary.profitPerHour,
                perKmValue = shift.summary.profitPerKm,
            )
        }
    }
}

@Composable
fun ProfitText(
    value: BigDecimal,
    fontSize: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = formatSignedBrl(value),
        color = profitColor(value),
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

@Composable
private fun profitColor(value: BigDecimal): Color =
    if (value.signum() < 0) MaterialTheme.colorScheme.error else ExpedientesTheme.colors.positive

@Composable
fun ShiftDetails(
    earnings: BigDecimal,
    fuelCost: BigDecimal,
    km: BigDecimal,
    minutes: Int,
    hourly: BigDecimal?,
    perKmValue: BigDecimal?,
) {
    val none = stringResource(R.string.profile_no_price)
    DetailList(
        items =
            listOf(
                stringResource(R.string.home_earnings) to formatBrl(earnings),
                stringResource(R.string.home_fuel) to formatBrl(fuelCost),
                stringResource(R.string.home_km) to formatKm(km),
                stringResource(R.string.home_time) to formatDuration(minutes),
                stringResource(R.string.home_per_hour) to (hourly?.let(::formatBrl) ?: none),
                stringResource(R.string.home_per_km) to (perKmValue?.let(::formatAmount) ?: none),
            ),
    )
}

@Composable
private fun dayLabel(
    date: LocalDate,
    today: LocalDate,
): String =
    when (val label = relativeDay(date, today)) {
        DayLabel.Today -> stringResource(R.string.home_today)
        DayLabel.Yesterday -> stringResource(R.string.home_yesterday)
        is DayLabel.Date -> label.text
    }

@Composable
private fun StartShiftError(
    flow: StartFlow,
    onOpenProfile: () -> Unit,
) {
    val error = flow as? StartFlow.Error ?: return
    Column {
        Text(
            text = error.failure.text(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("start_error"),
        )
        if (error.offerProfile) {
            TextButton(onClick = onOpenProfile) {
                Text(stringResource(R.string.home_go_profile), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
