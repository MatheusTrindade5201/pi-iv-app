package br.univesp.pi.expedientes.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.Underline),
        color = MaterialTheme.colorScheme.onBackground,
        modifier =
            modifier
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(vertical = 12.dp),
    )
}
