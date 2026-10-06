package br.univesp.pi.expedientes.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme

@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(24.dp))
        ScreenTitle(text = title)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.screen_under_construction),
            style = MaterialTheme.typography.bodyLarge,
            color = ExpedientesTheme.colors.textSecondary,
        )
    }
}
