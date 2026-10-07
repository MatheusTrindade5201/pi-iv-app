package br.univesp.pi.expedientes.core.ui.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.univesp.pi.expedientes.R

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.loading)
    CircularProgressIndicator(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { contentDescription = label }.testTag("loading"),
    )
}
