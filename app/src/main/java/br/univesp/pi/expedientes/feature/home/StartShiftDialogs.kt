package br.univesp.pi.expedientes.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.ui.components.ChoiceChips
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import br.univesp.pi.expedientes.feature.profile.fuelLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartShiftDialogs(
    state: StartFlow,
    actions: HomeActions,
    onOpenProfile: () -> Unit,
) {
    when (state) {
        StartFlow.NoVehicle -> NoVehicleDialog(onDismiss = actions::dismissStartFlow, onOpenProfile = onOpenProfile)
        is StartFlow.ChooseFuel ->
            ModalBottomSheet(
                onDismissRequest = actions::dismissStartFlow,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                ChooseFuelForm(
                    state = state,
                    onChoose = actions::chooseFuel,
                    onConfirm = actions::confirmFuel,
                    modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp),
                )
            }
        else -> Unit
    }
}

@Composable
fun NoVehicleDialog(
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_no_vehicle_title)) },
        text = { Text(stringResource(R.string.home_no_vehicle_text)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onOpenProfile()
                },
            ) {
                Text(stringResource(R.string.home_go_profile), color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
fun ChooseFuelForm(
    state: StartFlow.ChooseFuel,
    onChoose: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_choose_fuel),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_choose_fuel_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = ExpedientesTheme.colors.textSecondary,
        )
        ChoiceChips(
            options = listOf(FuelIds.GASOLINE, FuelIds.ETHANOL).map { it to fuelLabel(it) },
            selected = state.fuelId,
            onSelect = onChoose,
            enabled = !state.isStarting,
        )
        PrimaryButton(
            text = stringResource(R.string.home_start_shift),
            onClick = onConfirm,
            loading = state.isStarting,
            leadingIcon = { Icon(painterResource(R.drawable.ic_play), contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("confirm_fuel"),
        )
    }
}
