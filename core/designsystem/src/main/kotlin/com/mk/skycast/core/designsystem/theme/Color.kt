package com.mk.skycast.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Brand and weather-surface colors. Content drawn over the sky always uses these. */
object SkyColors {
    val Sun = Color(0xFFFFD184)
    val SunCore = Color(0xFFFFE6AB)
    val Ice = Color(0xFF9DE2F0)
    val DeepIce = Color(0xFF58B8DD)
    val Ink = Color(0xFF102E43)
    val OnSky = Color(0xFFF6FAFF)
    val MutedSky = Color(0xFFD4E5EF)

    val Moon = Color(0xFFDFE9FF)
    val Cloud = Color(0xFFE2EEF7)
    val StormCloud = Color(0xFF9DADC9)
    val FogLine = Color(0xFFCBDFEB)

    /** Translucent card surface over the animated sky. */
    val Glass = Color(0xFF0B243B).copy(alpha = 0.48f)
    val GlassBorder = Color.White.copy(alpha = 0.14f)
    val Divider = Color.White.copy(alpha = 0.10f)
    val Track = Color.White.copy(alpha = 0.12f)
    val SkyScrim = Color(0xFF061B30).copy(alpha = 0.12f)
    val Banner = Color(0xFF102C44).copy(alpha = 0.70f)

    /** Cold → warm, used by temperature range bars. */
    val TemperatureRange = listOf(Color(0xFF91D9EB), Color(0xFFFAD393), Color(0xFFF4A381))

    /** Good → hazardous, used by AQI and UV meters. */
    val Severity = listOf(
        Color(0xFF8DDEB0),
        Color(0xFFF6D978),
        Color(0xFFF1AC79),
        Color(0xFFEE7F8B),
        Color(0xFFBD9BDE),
        Color(0xFFC788AB),
    )
}

/** Vertical gradients for each sky, top → bottom. */
internal object SkyGradients {
    val ClearDay = listOf(Color(0xFF103A61), Color(0xFF23698C), Color(0xFF365F7A))
    val ClearNight = listOf(Color(0xFF0C1733), Color(0xFF232E59), Color(0xFF253957))
    val Cloudy = listOf(Color(0xFF233F58), Color(0xFF41657B), Color(0xFF274A64))
    val Rain = listOf(Color(0xFF162D42), Color(0xFF36566D), Color(0xFF243F57))
    val Snow = listOf(Color(0xFF344D66), Color(0xFF537087), Color(0xFF2F4B67))
    val Thunder = listOf(Color(0xFF171C36), Color(0xFF493B62), Color(0xFF24374F))
    val Fog = listOf(Color(0xFF3A505D), Color(0xFF526A75), Color(0xFF304858))
}

internal val LightColors = lightColorScheme(
    primary = Color(0xFF14677C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0EEF3),
    onPrimaryContainer = SkyColors.Ink,
    secondary = Color(0xFF80602D),
    secondaryContainer = Color(0xFFFFE3AE),
    background = Color(0xFFF2F6F8),
    onBackground = SkyColors.Ink,
    surface = Color(0xFFF8FBFC),
    onSurface = SkyColors.Ink,
    surfaceVariant = Color(0xFFE1EBEF),
    onSurfaceVariant = Color(0xFF47616E),
    surfaceContainer = Color(0xFFEAF1F4),
    surfaceContainerLow = Color.White,
    outline = Color(0xFF6F8791),
    outlineVariant = Color(0xFFCBDCE3),
)

internal val DarkColors = darkColorScheme(
    primary = SkyColors.Ice,
    onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF174E60),
    onPrimaryContainer = Color(0xFFD0EEF3),
    secondary = SkyColors.Sun,
    secondaryContainer = Color(0xFF594325),
    background = Color(0xFF091B29),
    onBackground = SkyColors.OnSky,
    surface = Color(0xFF102432),
    onSurface = SkyColors.OnSky,
    surfaceVariant = Color(0xFF233C49),
    onSurfaceVariant = Color(0xFFB6CCD7),
    surfaceContainer = Color(0xFF142D3B),
    surfaceContainerLow = Color(0xFF102432),
    outline = Color(0xFF819AA6),
    outlineVariant = Color(0xFF334C59),
)
