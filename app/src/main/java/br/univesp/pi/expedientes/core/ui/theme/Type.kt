package br.univesp.pi.expedientes.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

val Typography =
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Normal),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    )
