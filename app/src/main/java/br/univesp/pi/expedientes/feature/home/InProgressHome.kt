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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.format.formatDuration
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.components.SectionCard
import br.univesp.pi.expedientes.core.ui.components.StatusBanner
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import java.time.Instant

@Composable
fun InProgressHome(
    state: HomeUiState,
    shift: ShiftResponse,
    now: Instant,
    actions: ShiftActions,
    onResumeGps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            ShiftHeader(elapsedMinutes(shift, now))
            GpsBannerView(gpsBanner(state.gpsActive, lastPointAt(shift, state.lastFixAt), now), onResumeGps)
            PartialProfitCard(shift)
        }
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(
                text = stringResource(R.string.home_add_earning),
                onClick = actions::openEarning,
                leadingIcon = { Icon(painterResource(R.drawable.ic_plus), contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("open_earning"),
            )
            SecondaryButton(
                text = stringResource(R.string.home_finish_shift),
                icon = R.drawable.ic_stop_circle,
                onClick = actions::openFinish,
                modifier = Modifier.fillMaxWidth().testTag("open_finish"),
            )
        }
    }
}

@Composable
private fun ShiftHeader(minutes: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(ExpedientesTheme.colors.positive, CircleShape))
        Text(
            text = stringResource(R.string.home_in_progress),
            color = ExpedientesTheme.colors.positive,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        Text(
            text = formatDuration(minutes),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("elapsed"),
        )
    }
}

@Composable
fun PartialProfitCard(shift: ShiftResponse) {
    SectionCard {
        Text(
            text = stringResource(R.string.home_partial_profit),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally).semantics { heading() },
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

@Composable
private fun GpsBannerView(
    banner: GpsBanner,
    onResume: () -> Unit,
) {
    val active = banner !is GpsBanner.Inactive
    val text =
        when (banner) {
            is GpsBanner.Active -> stringResource(R.string.home_gps_active_ago, formatAgo(banner.secondsAgo))
            GpsBanner.Waiting -> stringResource(R.string.home_gps_waiting)
            is GpsBanner.Inactive ->
                banner.secondsAgo?.let { stringResource(R.string.home_gps_inactive_ago, formatAgo(it)) }
                    ?: stringResource(R.string.home_gps_inactive)
        }
    StatusBanner(
        text = text,
        icon = if (active) R.drawable.ic_clock else R.drawable.ic_warning,
        container = if (active) ExpedientesTheme.colors.positiveContainer else ExpedientesTheme.colors.warningContainer,
        content =
            if (active) ExpedientesTheme.colors.onPositiveContainer else ExpedientesTheme.colors.onWarningContainer,
        modifier = Modifier.testTag("gps_banner"),
    ) {
        if (banner is GpsBanner.Inactive && banner.canResume) {
            TextButton(onClick = onResume) {
                Text(
                    text = stringResource(R.string.home_gps_resume),
                    color = ExpedientesTheme.colors.onWarningContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(8.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Icon(painterResource(icon), contentDescription = null)
        Spacer(Modifier.size(12.dp))
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
