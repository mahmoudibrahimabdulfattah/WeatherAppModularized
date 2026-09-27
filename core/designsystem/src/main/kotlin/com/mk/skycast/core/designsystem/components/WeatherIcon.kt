package com.mk.skycast.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import com.mk.skycast.core.designsystem.theme.SkyColors
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Icons are drawn on a 100×100 virtual canvas and scaled to the available size. */
private const val CANVAS = 100f
private const val ICON_CYCLE_MS = 24_000
private const val SUN_RAYS = 8

/**
 * Canvas-drawn weather icon with subtle motion (rotating sun rays, bobbing rain).
 * Pure vector drawing: crisp at any size and no image assets.
 */
@Composable
fun WeatherIcon(condition: SkyCondition, isDay: Boolean, modifier: Modifier = Modifier, animated: Boolean = true) {
    val phase = rememberSkyPhase(animated && !rememberReducedMotion(), ICON_CYCLE_MS)
    val bolt = remember { boltPath() }
    Canvas(modifier) {
        val factor = min(size.width, size.height) / CANVAS
        translate((size.width - factor * CANVAS) / 2, (size.height - factor * CANVAS) / 2) {
            scale(factor, factor, Offset.Zero) {
                val t = phase.value
                if (condition.hasSun) {
                    val center = if (condition == SkyCondition.Clear) Offset(50f, 47f) else Offset(66f, 32f)
                    if (isDay) drawSun(center, t) else drawMoon(center)
                }
                if (condition != SkyCondition.Clear) {
                    drawCloud(if (condition == SkyCondition.Thunder) SkyColors.StormCloud else SkyColors.Cloud)
                }
                when (condition) {
                    SkyCondition.Rain -> drawRain(t)
                    SkyCondition.Snow -> drawSnow()
                    SkyCondition.Thunder -> drawPath(bolt, SkyColors.Sun)
                    SkyCondition.Fog -> drawFog()
                    else -> Unit
                }
            }
        }
    }
}

private fun boltPath() = Path().apply {
    moveTo(56f, 53f)
    lineTo(40f, 76f)
    lineTo(51f, 76f)
    lineTo(45f, 94f)
    lineTo(68f, 66f)
    lineTo(55f, 66f)
    close()
}

private fun DrawScope.drawSun(center: Offset, t: Float) {
    drawCircle(SkyColors.Sun.copy(alpha = 0.1f), 31f, center)
    rotate(t * 360, center) {
        repeat(SUN_RAYS) { i ->
            rotate(i * 360f / SUN_RAYS, center) {
                drawLine(SkyColors.Sun, center + Offset(0f, -25f), center + Offset(0f, -31f), 3f, StrokeCap.Round)
            }
        }
    }
    drawCircle(SkyColors.Sun, 19f, center)
    drawCircle(SkyColors.SunCore, 13f, center + Offset(-3f, -3f))
}

/** Crescent as a thick arc, so it never punches a hole through the backdrop. */
private fun DrawScope.drawMoon(center: Offset) {
    drawArc(
        color = SkyColors.Moon,
        startAngle = 55f,
        sweepAngle = 265f,
        useCenter = false,
        topLeft = center - Offset(20f, 20f),
        size = Size(40f, 40f),
        style = Stroke(13f, cap = StrokeCap.Round),
    )
    drawCircle(SkyColors.Sun, 2f, center + Offset(24f, -20f))
}

private fun DrawScope.drawCloud(color: Color) {
    drawCircle(color, 16f, Offset(32f, 53f))
    drawCircle(color, 22f, Offset(48f, 43f))
    drawCircle(color, 17f, Offset(68f, 53f))
    drawRoundRect(color, Offset(24f, 49f), Size(53f, 20f), CornerRadius(10f))
    drawCircle(Color.White.copy(alpha = 0.22f), 15f, Offset(44f, 39f))
}

private fun DrawScope.drawRain(t: Float) {
    repeat(3) { i ->
        val y = 76f + sin(t * PI * 4 + i).toFloat() * 2
        drawLine(SkyColors.Ice, Offset(34f + i * 17, y), Offset(30f + i * 17, y + 9), 4f, StrokeCap.Round)
    }
}

private fun DrawScope.drawSnow() {
    repeat(3) { i ->
        val center = Offset(32f + i * 19, 81f + (i % 2) * 5)
        repeat(3) { arm ->
            rotate(arm * 60f, center) {
                drawLine(Color.White, center - Offset(0f, 5f), center + Offset(0f, 5f), 2f, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawFog() {
    repeat(3) { i ->
        val y = 76f + i * 7
        drawLine(SkyColors.FogLine, Offset(20f + i * 5, y), Offset(75f - i * 5, y), 3f, StrokeCap.Round)
    }
}
