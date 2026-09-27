package com.mk.skycast.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkyGradients
import kotlin.math.PI
import kotlin.math.sin

private const val BACKDROP_CYCLE_MS = 18_000
private const val STAR_COUNT = 32
private const val CLOUD_COUNT = 5
private const val DROP_COUNT = 28
private const val RAIN_SPEED = 5
private const val SNOW_SPEED = 1
private const val FLASH_START = 0.94f
private const val FLASH_END = 0.95f
private const val TWO_PI = PI * 2

/**
 * Full-bleed animated sky: gradient per condition and time of day, with stars,
 * drifting clouds, rain, snow and lightning. Particle positions are derived
 * deterministically from their index, so drawing allocates nothing per frame.
 */
@Composable
fun WeatherBackdrop(condition: SkyCondition, isDay: Boolean, modifier: Modifier = Modifier, animated: Boolean = true) {
    val phase = rememberSkyPhase(animated && !rememberReducedMotion(), BACKDROP_CYCLE_MS)
    val background = remember(condition, isDay) { Brush.verticalGradient(skyGradient(condition, isDay)) }
    val cloudBrush = remember { unitGlow(Color.White.copy(alpha = 0.09f)) }
    val sunBrush = remember { unitGlow(SkyColors.Sun.copy(alpha = 0.10f)) }

    Box(modifier.background(background)) {
        Canvas(Modifier.fillMaxSize()) {
            val t = phase.value
            if (!isDay && condition.hasSun) drawStars(t)
            if (condition.hasSun) drawSunGlow(sunBrush)
            if (condition != SkyCondition.Clear) drawClouds(cloudBrush, t)
            if (condition.hasPrecipitation) drawPrecipitation(snow = condition == SkyCondition.Snow, t = t)
            if (condition == SkyCondition.Thunder && t > FLASH_START && t < FLASH_END) {
                drawRect(Color.White.copy(alpha = 0.07f))
            }
            drawRect(SkyColors.SkyScrim)
        }
    }
}

private fun skyGradient(condition: SkyCondition, isDay: Boolean): List<Color> = when {
    condition == SkyCondition.Thunder -> SkyGradients.Thunder
    condition == SkyCondition.Rain -> SkyGradients.Rain
    condition == SkyCondition.Snow -> SkyGradients.Snow
    condition == SkyCondition.Fog -> SkyGradients.Fog
    !isDay -> SkyGradients.ClearNight
    condition == SkyCondition.Cloud || condition == SkyCondition.Unknown -> SkyGradients.Cloudy
    else -> SkyGradients.ClearDay
}

/** Radial glow of radius 1 at the origin; scaled at draw time. */
private fun unitGlow(color: Color) =
    Brush.radialGradient(listOf(color, Color.Transparent), center = Offset.Zero, radius = 1f)

/** Pseudo-random but stable 0..1 value for particle [index]. */
private fun spread(index: Int, multiplier: Int, offset: Int, modulo: Int): Float =
    ((index * multiplier + offset) % modulo) / modulo.toFloat()

private fun DrawScope.drawStars(t: Float) {
    repeat(STAR_COUNT) { i ->
        val x = spread(i, 73, 19, 101) * size.width
        val y = spread(i, 37, 11, 97) * size.height * 0.68f
        val twinkle = (sin(t * TWO_PI + i).toFloat() + 1) / 2
        val radius = if (i % 3 == 0) 1.6.dp.toPx() else 0.8.dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.25f + 0.4f * twinkle), radius, Offset(x, y))
    }
}

private fun DrawScope.drawSunGlow(brush: Brush) {
    val radius = size.width * 0.62f
    translate(size.width * 0.84f, size.height * 0.15f) {
        scale(radius, radius, Offset.Zero) { drawCircle(brush, 1f, Offset.Zero) }
    }
}

private fun DrawScope.drawClouds(brush: Brush, t: Float) {
    repeat(CLOUD_COUNT) { i ->
        val drift = sin((t + i * 0.15f) * TWO_PI).toFloat() * size.width * 0.07f
        val left = size.width * (0.25f + (i % 2) * 0.55f) + drift
        val top = size.height * (0.08f + i * 0.16f)
        translate(left, top) {
            scale(size.width * 0.9f, size.height * 0.16f, Offset.Zero) { drawCircle(brush, 1f, Offset.Zero) }
        }
    }
}

private fun DrawScope.drawPrecipitation(snow: Boolean, t: Float) {
    val speed = if (snow) SNOW_SPEED else RAIN_SPEED
    repeat(DROP_COUNT) { i ->
        val x = spread(i, 47, 7, 101) * size.width
        val y = ((i * 0.137f + t * speed) % 1f) * size.height
        if (snow) {
            val sway = sin(t * TWO_PI + i).toFloat() * 18.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.35f), (1 + i % 3).dp.toPx(), Offset(x + sway, y))
        } else {
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(x, y),
                end = Offset(x - 5.dp.toPx(), y + 22.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
