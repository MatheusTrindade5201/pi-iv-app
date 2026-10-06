package br.univesp.pi.expedientes.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtendedColors(
    val positive: Color,
    val positiveContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val selectedContainer: Color,
    val textSecondary: Color,
    val navUnselected: Color,
)

private val extendedColors =
    ExtendedColors(
        positive = Green,
        positiveContainer = GreenContainer,
        warningContainer = WarningContainer,
        onWarningContainer = OnWarningContainer,
        selectedContainer = OrangeSelected,
        textSecondary = TextSecondary,
        navUnselected = NavUnselected,
    )

private val colorScheme =
    darkColorScheme(
        primary = Orange,
        onPrimary = OnOrange,
        secondary = Orange,
        onSecondary = OnOrange,
        background = Black,
        onBackground = White,
        surface = Black,
        onSurface = White,
        surfaceContainer = Surface,
        surfaceContainerHigh = Surface,
        surfaceContainerLow = Surface,
        surfaceVariant = Surface,
        onSurfaceVariant = TextSecondary,
        outline = Outline,
        outlineVariant = Outline,
        error = Red,
    )

val LocalExtendedColors = staticCompositionLocalOf { extendedColors }

@Composable
fun ExpedientesTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

object ExpedientesTheme {
    val colors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
