package com.mk.skycast.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private fun style(weight: FontWeight, size: TextUnit, lineHeight: TextUnit, letterSpacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

/** Expressive scale: a huge, thin hero temperature over calm, readable body text. */
internal val SkyTypography = Typography(
    displayLarge = style(FontWeight.Light, 120.sp, 124.sp, (-4).sp),
    displayMedium = style(FontWeight.Light, 56.sp, 64.sp, (-2).sp),
    headlineLarge = style(FontWeight.Medium, 32.sp, 40.sp, (-0.8).sp),
    headlineSmall = style(FontWeight.Medium, 24.sp, 32.sp),
    titleLarge = style(FontWeight.Medium, 22.sp, 28.sp),
    titleMedium = style(FontWeight.Medium, 16.sp, 24.sp),
    bodyLarge = style(FontWeight.Normal, 16.sp, 24.sp),
    bodyMedium = style(FontWeight.Normal, 14.sp, 20.sp),
    labelLarge = style(FontWeight.Medium, 14.sp, 20.sp),
    labelSmall = style(FontWeight.Medium, 11.sp, 16.sp),
)
