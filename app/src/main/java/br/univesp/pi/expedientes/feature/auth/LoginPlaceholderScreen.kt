package br.univesp.pi.expedientes.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.ui.components.PrimaryButton
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme

@Composable
fun LoginPlaceholderScreen(
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(text = stringResource(R.string.login_title), style = MaterialTheme.typography.displayMedium)
        Text(
            text = stringResource(R.string.login_placeholder_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = ExpedientesTheme.colors.textSecondary,
        )
        PrimaryButton(
            text = stringResource(R.string.login_enter),
            onClick = onEnter,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
