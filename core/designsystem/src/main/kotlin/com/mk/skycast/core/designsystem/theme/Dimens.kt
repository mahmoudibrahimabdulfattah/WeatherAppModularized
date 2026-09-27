package com.mk.skycast.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 8pt spacing grid plus a few layout constants. */
object SkySpace {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp

    /** Minimum accessible touch target. */
    val touch = 48.dp
    val cardRadius = 28.dp

    /** Width at which screens switch to a two-pane layout. */
    val paneBreakpoint = 600.dp

    /** Max width of single-column content on large screens. */
    val contentMaxWidth = 720.dp
}

/** Icon sizes used across screens. */
object SkyIconSize {
    val small = 18.dp
    val medium = 24.dp
    val large = 36.dp
    val hero = 104.dp
    val illustration = 160.dp
}

internal val SkyShapes = Shapes(
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(SkySpace.cardRadius),
)
