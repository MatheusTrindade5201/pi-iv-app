package br.univesp.pi.expedientes.feature.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.ui.components.LabeledTextField
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.components.StatusBanner
import br.univesp.pi.expedientes.core.ui.components.TextLink
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme

data class AuthFieldSpec(
    val field: AuthField,
    @param:StringRes val label: Int,
    @param:StringRes val placeholder: Int,
    val keyboardType: KeyboardType,
    val isPassword: Boolean = false,
)

val NameSpec = AuthFieldSpec(AuthField.Name, R.string.field_name, R.string.placeholder_name, KeyboardType.Text)
val EmailSpec = AuthFieldSpec(AuthField.Email, R.string.field_email, R.string.placeholder_email, KeyboardType.Email)
val PasswordSpec =
    AuthFieldSpec(
        AuthField.Password,
        R.string.field_password,
        R.string.placeholder_password,
        KeyboardType.Password,
        true,
    )
val PasswordConfirmSpec =
    AuthFieldSpec(
        AuthField.PasswordConfirm,
        R.string.field_password_confirm,
        R.string.placeholder_password,
        KeyboardType.Password,
        true,
    )

@Composable
fun AuthForm(
    title: String,
    fields: List<AuthFieldSpec>,
    state: AuthFormState,
    onChange: (AuthField, String) -> Unit,
    onSubmit: () -> Unit,
    submitLabel: String,
    linkLabel: String,
    onLink: () -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(72.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Spacer(Modifier.height(if (notice == null) 56.dp else 24.dp))
        notice?.let {
            StatusBanner(
                text = it,
                icon = R.drawable.ic_warning,
                container = ExpedientesTheme.colors.warningContainer,
                content = ExpedientesTheme.colors.onWarningContainer,
                modifier = Modifier.widthIn(max = 480.dp).padding(bottom = 24.dp).testTag("auth_notice"),
            )
        }
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            fields.forEachIndexed { index, spec ->
                val isLast = index == fields.lastIndex
                LabeledTextField(
                    label = stringResource(spec.label),
                    value = state.value(spec.field),
                    onValueChange = { onChange(spec.field, it) },
                    placeholder = stringResource(spec.placeholder),
                    error = state.fieldErrors[spec.field]?.text(),
                    isPassword = spec.isPassword,
                    enabled = !state.isSubmitting,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = spec.keyboardType,
                            imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
                        ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    testTag = "field_${spec.field.name}",
                )
            }
            state.generalError?.let {
                Text(
                    text = it.text(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("general_error"),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextLink(text = linkLabel, onClick = onLink, enabled = !state.isSubmitting)
                PrimaryButton(
                    text = submitLabel,
                    onClick = onSubmit,
                    loading = state.isSubmitting,
                    modifier = Modifier.widthIn(min = 160.dp).testTag("submit"),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
