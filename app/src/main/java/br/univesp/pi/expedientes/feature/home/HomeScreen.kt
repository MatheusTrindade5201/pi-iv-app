package br.univesp.pi.expedientes.feature.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import br.univesp.pi.expedientes.core.ui.components.DetailList
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.components.SectionCard
import br.univesp.pi.expedientes.core.ui.components.text
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.feature.tracking.LocationPermissions
import br.univesp.pi.expedientes.feature.tracking.PermissionPrompt
import br.univesp.pi.expedientes.feature.tracking.permissionPromptAfterDenial
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Composable
fun HomeEntry(
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    var prompt by remember { mutableStateOf<PermissionPrompt?>(null) }
    val inProgress = state.content is HomeContent.InProgress
    val now by produceState(Instant.now()) {
        while (true) {
            delay(TICK_MS)
            value = Instant.now()
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(inProgress) {
        if (inProgress) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(REFRESH_MS)
                    viewModel.refresh()
                }
            }
        }
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                prompt = null
                if (inProgress) viewModel.resumeTracking() else viewModel.startShift()
            } else {
                val rationale =
                    activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            it,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                    } == true
                prompt = permissionPromptAfterDenial(rationale)
            }
        }
    LaunchedEffect(Unit) { viewModel.refresh() }
    HomeScreen(
        state = state,
        actions = viewModel,
        shiftActions = viewModel,
        now = now,
        onOpenProfile = onOpenProfile,
        onResumeGps = {
            if (LocationPermissions.hasPreciseLocation(context)) {
                viewModel.resumeTracking()
            } else {
                launcher.launch(LocationPermissions.requested)
            }
        },
        onStartClick = {
            if (LocationPermissions.hasPreciseLocation(context)) {
                prompt = null
                viewModel.startShift()
            } else {
                launcher.launch(LocationPermissions.requested)
            }
        },
        permissionPrompt = prompt,
        onPermissionAction = {
            when (prompt) {
                PermissionPrompt.Ask -> launcher.launch(LocationPermissions.requested)
                PermissionPrompt.OpenSettings ->
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                null -> Unit
            }
        },
    )
    StartShiftDialogs(state = state.startFlow, actions = viewModel, onOpenProfile = onOpenProfile)
    ShiftSheets(state = state, now = now, actions = viewModel)
}

private const val TICK_MS = 1_000L
private const val REFRESH_MS = 30_000L

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    shiftActions: ShiftActions,
    now: Instant,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onResumeGps: () -> Unit = shiftActions::resumeTracking,
    onStartClick: () -> Unit = actions::startShift,
    permissionPrompt: PermissionPrompt? = null,
    onPermissionAction: () -> Unit = {},
) {
    when (val content = state.content) {
        HomeContent.Loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        is HomeContent.Failed -> FailedHome(content.failure, actions::retry, modifier)
        is HomeContent.Idle ->
            IdleHome(state, content, onStartClick, onOpenProfile, permissionPrompt, onPermissionAction, modifier)
        is HomeContent.InProgress -> InProgressHome(state, content.shift, now, shiftActions, onResumeGps, modifier)
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
    onStartClick: () -> Unit,
    onOpenProfile: () -> Unit,
    permissionPrompt: PermissionPrompt?,
    onPermissionAction: () -> Unit,
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
            permissionPrompt?.let { PermissionMessage(it, onPermissionAction) }
            PrimaryButton(
                text = stringResource(R.string.home_start_shift),
                onClick = onStartClick,
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

@Composable
private fun PermissionMessage(
    prompt: PermissionPrompt,
    onAction: () -> Unit,
) {
    Column(modifier = Modifier.testTag("permission_prompt")) {
        Text(
            text = stringResource(R.string.permission_location_needed),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onAction) {
            Text(
                text =
                    stringResource(
                        if (prompt ==
                            PermissionPrompt.Ask
                        ) {
                            R.string.permission_allow
                        } else {
                            R.string.permission_open_settings
                        },
                    ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
