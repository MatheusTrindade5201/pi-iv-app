package br.univesp.pi.expedientes.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.format.formatBrl
import br.univesp.pi.expedientes.core.format.formatDecimal
import br.univesp.pi.expedientes.core.network.RequestFailure
import br.univesp.pi.expedientes.core.network.api.VehicleResponse
import br.univesp.pi.expedientes.core.ui.components.DashedActionCard
import br.univesp.pi.expedientes.core.ui.components.DetailList
import br.univesp.pi.expedientes.core.ui.components.LabeledTextField
import br.univesp.pi.expedientes.core.ui.components.LoadingIndicator
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.components.ScreenTitle
import br.univesp.pi.expedientes.core.ui.components.SectionCard
import br.univesp.pi.expedientes.core.ui.components.TextLink
import br.univesp.pi.expedientes.core.ui.components.ValueTile
import br.univesp.pi.expedientes.core.ui.components.text
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme
import java.math.BigDecimal

@Composable
fun ProfileEntry(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreen(state = state, actions = viewModel)
    ProfileSheets(state = state, actions = viewModel)
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.height(0.dp))
        ScreenTitle(text = stringResource(R.string.profile_title))
        when {
            state.isLoading -> LoadingBox()
            state.loadFailure != null -> LoadFailure(state.loadFailure, actions::retry)
            else -> {
                NameField(state, actions)
                VehicleSection(state.vehicle, actions::openVehicleSheet)
                PricesSection(state.prices, actions::openPricesSheet)
                TextLink(
                    text = stringResource(R.string.profile_logout),
                    onClick = actions::logout,
                    modifier = Modifier.testTag("logout"),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}

@Composable
private fun LoadFailure(
    failure: RequestFailure,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = failure.text(), color = MaterialTheme.colorScheme.error)
        PrimaryButton(text = stringResource(R.string.profile_load_error_retry), onClick = onRetry)
    }
}

@Composable
private fun NameField(
    state: ProfileUiState,
    actions: ProfileActions,
) {
    val status = state.nameStatus
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LabeledTextField(
            label = stringResource(R.string.profile_name),
            value = state.name,
            onValueChange = actions::onNameChange,
            error =
                when (status) {
                    is NameStatus.Invalid -> status.error.text()
                    is NameStatus.Failed -> status.failure.text()
                    else -> null
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { actions.saveName() }),
            testTag = "profile_name",
            modifier = Modifier.onFocusChanged { if (!it.isFocused) actions.saveName() },
        )
        val hint =
            when (status) {
                NameStatus.Saving -> stringResource(R.string.profile_name_saving)
                NameStatus.Saved -> stringResource(R.string.profile_name_saved)
                else -> null
            }
        if (hint != null) {
            Text(text = hint, style = MaterialTheme.typography.bodySmall, color = ExpedientesTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun VehicleSection(
    vehicle: VehicleResponse?,
    onOpen: () -> Unit,
) {
    if (vehicle == null) {
        DashedActionCard(
            text = stringResource(R.string.profile_add_vehicle),
            onClick = onOpen,
            modifier = Modifier.testTag("add_vehicle"),
        )
        return
    }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(R.string.profile_my_vehicle), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = vehicle.model,
                style = MaterialTheme.typography.bodyLarge,
                color = ExpedientesTheme.colors.textSecondary,
            )
        }
        DetailList(
            items =
                listOf(
                    stringResource(R.string.profile_fuel) to fuelLabel(vehicle.fuelId),
                    stringResource(R.string.profile_average_consumption) to
                        stringResource(R.string.profile_kml_value, formatDecimal(vehicle.declaredKml)),
                ),
        )
        PrimaryButton(
            text = stringResource(R.string.profile_update_vehicle),
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().testTag("update_vehicle"),
        )
    }
}

@Composable
private fun PricesSection(
    prices: Map<String, BigDecimal>,
    onEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.profile_fuel_values),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_prices")) {
                Icon(
                    painterResource(R.drawable.ic_pencil),
                    contentDescription = stringResource(R.string.profile_edit_fuel_values),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PRICED_FUELS.forEach { fuelId ->
                ValueTile(
                    title = fuelLabel(fuelId),
                    value = prices[fuelId]?.let(::formatBrl) ?: stringResource(R.string.profile_no_price),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheets(
    state: ProfileUiState,
    actions: ProfileActions,
) {
    state.vehicleSheet?.let { form ->
        ModalBottomSheet(
            onDismissRequest = actions::closeVehicleSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            VehicleForm(
                state = form,
                onChange = actions::onVehicleChange,
                onFuelChange = actions::onVehicleFuelChange,
                onMeasureHelp = { actions.setMeasureHelpVisible(true) },
                onSave = actions::saveVehicle,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp),
            )
        }
    }
    state.pricesSheet?.let { form ->
        ModalBottomSheet(
            onDismissRequest = actions::closePricesSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            PricesForm(
                state = form,
                onChange = actions::onPriceChange,
                onSave = actions::savePrices,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp),
            )
        }
    }
    if (state.showMeasureHelp) {
        AlertDialog(
            onDismissRequest = { actions.setMeasureHelpVisible(false) },
            confirmButton = {
                TextButton(onClick = { actions.setMeasureHelpVisible(false) }) {
                    Text(stringResource(R.string.close))
                }
            },
            title = { Text(stringResource(R.string.measure_help_title)) },
            text = { Text(stringResource(R.string.measure_help_text)) },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}
