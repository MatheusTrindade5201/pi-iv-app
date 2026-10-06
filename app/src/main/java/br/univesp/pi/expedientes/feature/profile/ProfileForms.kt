package br.univesp.pi.expedientes.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.network.api.FuelIds
import br.univesp.pi.expedientes.core.ui.components.ChoiceChips
import br.univesp.pi.expedientes.core.ui.components.LabeledTextField
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.components.text

@Composable
fun fuelLabel(fuelId: String): String =
    stringResource(
        when (fuelId) {
            FuelIds.ETHANOL -> R.string.fuel_ethanol
            FuelIds.FLEX -> R.string.fuel_flex
            else -> R.string.fuel_gasoline
        },
    )

@Composable
fun ProfileError.text(): String =
    stringResource(
        when (this) {
            ProfileError.BlankName -> R.string.error_blank_name
            ProfileError.LongName -> R.string.error_long_name
            ProfileError.BlankModel -> R.string.error_model_blank
            ProfileError.LongModel -> R.string.error_model_long
            ProfileError.InvalidKml -> R.string.error_kml
            ProfileError.InvalidDisplacement -> R.string.error_displacement
            ProfileError.InvalidYear -> R.string.error_year
            ProfileError.InvalidPrice -> R.string.error_price
            ProfileError.NoPrice -> R.string.error_no_price
        },
    )

@Composable
fun VehicleForm(
    state: VehicleFormState,
    onChange: (VehicleField, String) -> Unit,
    onFuelChange: (String) -> Unit,
    onMeasureHelp: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
    val enabled = !state.isSaving
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text =
                stringResource(
                    if (state.isEditing) R.string.vehicle_sheet_update else R.string.vehicle_sheet_create,
                ),
            style = MaterialTheme.typography.titleLarge,
        )
        LabeledTextField(
            label = stringResource(R.string.vehicle_model),
            value = state.model,
            onValueChange = { onChange(VehicleField.Model, it) },
            placeholder = stringResource(R.string.vehicle_model_placeholder),
            error = state.errors[VehicleField.Model]?.text(),
            enabled = enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            testTag = "vehicle_model",
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(R.string.profile_fuel), style = MaterialTheme.typography.bodyLarge)
            ChoiceChips(
                options = listOf(FuelIds.GASOLINE, FuelIds.ETHANOL, FuelIds.FLEX).map { it to fuelLabel(it) },
                selected = state.fuelId,
                onSelect = onFuelChange,
                enabled = enabled,
            )
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.profile_average_consumption),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onMeasureHelp) {
                    Text(stringResource(R.string.vehicle_how_to_measure), color = MaterialTheme.colorScheme.primary)
                }
            }
            LabeledTextField(
                label = "",
                value = state.kml,
                onValueChange = { onChange(VehicleField.Kml, it) },
                placeholder = stringResource(R.string.vehicle_kml_placeholder),
                error = state.errors[VehicleField.Kml]?.text(),
                enabled = enabled,
                keyboardOptions = numberKeyboard,
                trailingText = stringResource(R.string.vehicle_kml_suffix),
                testTag = "vehicle_kml",
                showLabel = false,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LabeledTextField(
                label = stringResource(R.string.vehicle_displacement),
                value = state.displacement,
                onValueChange = { onChange(VehicleField.Displacement, it) },
                placeholder = stringResource(R.string.vehicle_optional),
                error = state.errors[VehicleField.Displacement]?.text(),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                testTag = "vehicle_displacement",
                modifier = Modifier.weight(1f),
            )
            LabeledTextField(
                label = stringResource(R.string.vehicle_year),
                value = state.year,
                onValueChange = { onChange(VehicleField.Year, it) },
                placeholder = stringResource(R.string.vehicle_optional),
                error = state.errors[VehicleField.Year]?.text(),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                testTag = "vehicle_year",
                modifier = Modifier.weight(1f),
            )
        }
        state.failure?.let {
            Text(text = it.text(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(
            text = stringResource(R.string.save),
            onClick = onSave,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("vehicle_save"),
        )
    }
}

@Composable
fun PricesForm(
    state: PricesFormState,
    onChange: (String, String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.profile_fuel_values), style = MaterialTheme.typography.titleLarge)
        PRICED_FUELS.forEachIndexed { index, fuelId ->
            LabeledTextField(
                label = fuelLabel(fuelId),
                value = state.values[fuelId].orEmpty(),
                onValueChange = { onChange(fuelId, it) },
                placeholder = stringResource(R.string.price_placeholder),
                error = state.errors[fuelId]?.text(),
                enabled = !state.isSaving,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = if (index == PRICED_FUELS.lastIndex) ImeAction.Done else ImeAction.Next,
                    ),
                testTag = "price_$fuelId",
            )
        }
        state.failure?.let {
            Text(text = it.text(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        PrimaryButton(
            text = stringResource(R.string.save),
            onClick = onSave,
            loading = state.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("prices_save"),
        )
    }
}
