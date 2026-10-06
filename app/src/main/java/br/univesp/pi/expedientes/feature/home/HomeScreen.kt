package br.univesp.pi.expedientes.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.network.ApiStatus
import br.univesp.pi.expedientes.core.ui.components.ScreenTitle
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme

@Composable
fun HomeEntry(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onRetry = viewModel::checkApi)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(24.dp))
        ScreenTitle(text = stringResource(R.string.home_greeting))
        Spacer(Modifier.height(16.dp))
        ApiStatusCard(state = state, onRetry = onRetry)
    }
}

@Composable
private fun ApiStatusCard(
    state: HomeUiState,
    onRetry: () -> Unit,
) {
    val (label, color) =
        when (state.apiStatus) {
            null -> stringResource(R.string.home_api_checking) to ExpedientesTheme.colors.textSecondary
            ApiStatus.Connected -> stringResource(R.string.home_api_connected) to ExpedientesTheme.colors.positive
            ApiStatus.Unavailable -> stringResource(R.string.home_api_unavailable) to MaterialTheme.colorScheme.error
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_api_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            modifier = Modifier.testTag("api_status"),
        )
        Text(
            text = state.apiBaseUrl,
            style = MaterialTheme.typography.bodySmall,
            color = ExpedientesTheme.colors.textSecondary,
        )
        if (state.apiStatus == ApiStatus.Unavailable) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.home_api_retry))
            }
        }
    }
}
