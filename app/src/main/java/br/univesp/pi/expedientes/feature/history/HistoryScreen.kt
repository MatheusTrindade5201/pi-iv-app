package br.univesp.pi.expedientes.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.ui.components.PlaceholderScreen

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.history_title), modifier = modifier)
}
