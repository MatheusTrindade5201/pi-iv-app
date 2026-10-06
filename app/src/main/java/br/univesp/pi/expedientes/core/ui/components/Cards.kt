package br.univesp.pi.expedientes.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.univesp.pi.expedientes.R
import br.univesp.pi.expedientes.core.ui.theme.ExpedientesTheme

private val CardShape = RoundedCornerShape(8.dp)

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, CardShape)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun DetailList(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, CardShape)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(5.dp)
                        .background(MaterialTheme.colorScheme.onBackground, CircleShape),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
                Text(text = value, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
fun DashedActionCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, CardShape)
                .drawBehind {
                    drawRoundRect(
                        color = outline,
                        style =
                            Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                            ),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                    )
                }.clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(painterResource(R.drawable.ic_plus_circle), contentDescription = null)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun RowScope.ValueTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.background, CardShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CardShape)
                .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            val isSelected = id == selected
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .background(
                            if (isSelected) {
                                ExpedientesTheme.colors.selectedContainer
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            CardShape,
                        ).then(
                            if (isSelected) {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CardShape)
                            } else {
                                Modifier
                            },
                        ).selectable(
                            selected = isSelected,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelect(id) },
                        ).testTag("chip_$id")
                        .padding(horizontal = if (compact) 4.dp else 16.dp, vertical = 14.dp),
                contentAlignment = if (compact) Alignment.Center else Alignment.CenterStart,
            ) {
                Text(
                    text = label,
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
            }
        }
    }
}
