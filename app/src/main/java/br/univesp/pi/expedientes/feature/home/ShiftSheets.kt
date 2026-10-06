package br.univesp.pi.expedientes.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.format.formatBrl
import br.univesp.pi.expedientes.core.format.formatDuration
import br.univesp.pi.expedientes.core.network.api.ShiftResponse
import br.univesp.pi.expedientes.core.ui.components.ChoiceChips
import br.univesp.pi.expedientes.core.ui.components.LabeledTextField
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.components.text
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import java.math.BigDecimal
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftSheets(
    state: HomeUiState,
    now: Instant,
    actions: ShiftActions,
) {
    val shift = (state.content as? HomeContent.InProgress)?.shift ?: return
    state.earning?.let { earning ->
        ModalBottomSheet(
            onDismissRequest = actions::dismissEarning,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            EarningForm(earning, actions, Modifier.navigationBarsPadding().padding(bottom = 24.dp))
        }
    }
    state.finish?.let { finish ->
        ModalBottomSheet(
            onDismissRequest = actions::dismissFinish,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            FinishForm(
                flow = finish,
                shift = shift,
                elapsedMinutes = elapsedMinutes(shift, now),
                inactiveMinutes = inactiveMinutes(shift, state.lastFixAt, now),
                actions = actions,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
fun EarningForm(
    flow: EarningFlow,
    actions: ShiftActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.home_add_earning), style = MaterialTheme.typography.titleLarge)
        Text(
            text = formatBrl(flow.amount ?: BigDecimal.ZERO),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("earning_amount"),
        )
        ChoiceChips(
            options = EARNING_PRESETS.map { it.toPlainString() to formatBrl(it) },
            selected =
                EARNING_PRESETS
                    .firstOrNull {
                        it.compareTo(
                            flow.amount ?: BigDecimal.ZERO,
                        ) == 0
                    }?.toPlainString() ?: "",
            onSelect = { actions.choosePreset(BigDecimal(it)) },
            enabled = !flow.saving,
            compact = true,
        )
        LabeledTextField(
            label = stringResource(R.string.earning_other),
            value = flow.text,
            onValueChange = actions::changeEarning,
            enabled = !flow.saving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.testTag("earning_input"),
        )
        flow.failure?.let {
            Text(text = it.text(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(
            text = stringResource(R.string.home_add_earning),
            onClick = actions::addEarning,
            enabled = flow.amount != null,
            loading = flow.saving,
            leadingIcon = { Icon(painterResource(R.drawable.ic_plus), contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("add_earning"),
        )
        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(
                onClick = actions::overwriteEarning,
                enabled = flow.lastEarning != null && flow.amount != null && !flow.saving,
                modifier = Modifier.testTag("overwrite_earning"),
            ) {
                Icon(painterResource(R.drawable.ic_refresh), contentDescription = null)
                Text(
                    text = stringResource(R.string.earning_overwrite),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            flow.lastEarning?.let {
                Text(
                    text = stringResource(R.string.earning_last, formatBrl(it.amount)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExpedientesTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
fun FinishForm(
    flow: FinishFlow,
    shift: ShiftResponse,
    elapsedMinutes: Int,
    inactiveMinutes: Int,
    actions: ShiftActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.finish_title), style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.finish_elapsed),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatDuration(elapsedMinutes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        if (inactiveMinutes > 0) {
            StatusBanner(
                text = stringResource(R.string.finish_gps_gap, inactiveMinutes),
                icon = R.drawable.ic_warning,
                container = ExpedientesTheme.colors.warningContainer,
                content = ExpedientesTheme.colors.onWarningContainer,
                modifier = Modifier.testTag("gps_gap"),
            )
        }
        PartialProfitCard(shift)
        flow.failure?.let {
            Text(text = it.text(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(
            text = stringResource(R.string.finish_confirm),
            onClick = actions::confirmFinish,
            loading = flow.finishing,
            modifier = Modifier.fillMaxWidth().testTag("confirm_finish"),
        )
        TextButton(
            onClick = actions::dismissFinish,
            enabled = !flow.finishing,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Icon(painterResource(R.drawable.ic_refresh), contentDescription = null)
            Text(
                text = stringResource(R.string.finish_continue),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
