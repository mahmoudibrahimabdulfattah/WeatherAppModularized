package com.mk.skycast.feature.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.GlassCard
import com.mk.skycast.core.designsystem.components.SkyHeading
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Icon + heading row at the top of every card. */
@Composable
internal fun CardTitle(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    tint: Color = SkyColors.MutedSky,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkySpace.small),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(SkyIconSize.small), tint = tint)
        SkyHeading(title)
    }
}

/** Two equally sized tiles side by side, matching the taller one's height. */
@Composable
internal fun TileRow(
    start: @Composable (Modifier) -> Unit,
    end: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
    ) {
        start(Modifier.weight(1f).fillMaxHeight())
        end(Modifier.weight(1f).fillMaxHeight())
    }
}

/** A single metric: label, big value, and optional supporting content. Read as one unit by TalkBack. */
@Composable
internal fun DetailTile(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    GlassCard(modifier.heightIn(min = 170.dp).semantics(mergeDescendants = true) {}) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkySpace.small),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(SkyIconSize.small),
                tint = SkyColors.MutedSky,
            )
            Text(title, style = MaterialTheme.typography.labelLarge, color = SkyColors.MutedSky)
        }
        Text(value, style = MaterialTheme.typography.headlineSmall)
        content()
    }
}

@Composable
internal fun SupportingText(text: String, modifier: Modifier = Modifier, color: Color = SkyColors.MutedSky) {
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = color)
}

/** Gradient track with a marker at [progress] (0..1). Mirrored in RTL. */
@Composable
internal fun ScaleMeter(progress: Float, colors: List<Color>, modifier: Modifier = Modifier) {
    val gradient = remember(colors) { Brush.horizontalGradient(colors) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.fillMaxWidth().height(16.dp).graphicsLayer { scaleX = if (rtl) -1f else 1f }) {
        val inset = 4.dp.toPx()
        val y = size.height / 2
        drawLine(gradient, Offset(inset, y), Offset(size.width - inset, y), 5.dp.toPx(), StrokeCap.Round)
        val marker = Offset(inset + (size.width - inset * 2) * progress.coerceIn(0f, 1f), y)
        drawCircle(SkyColors.Ink, 4.dp.toPx(), marker)
        drawCircle(Color.White, 2.5.dp.toPx(), marker)
    }
}

/** Half-ellipse from sunrise to sunset with the sun placed at [progress] (0..1). */
@Composable
internal fun SunArc(progress: Float, isDaylight: Boolean, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.fillMaxWidth().height(90.dp).graphicsLayer { scaleX = if (rtl) -1f else 1f }) {
        val baseline = size.height - 12.dp.toPx()
        val width = size.width - 32.dp.toPx()
        val height = baseline - 8.dp.toPx()
        val topLeft = Offset((size.width - width) / 2, baseline - height)
        val arcSize = Size(width, height * 2)
        val dash = 5.dp.toPx()

        drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, baseline), Offset(size.width, baseline), 1.dp.toPx())
        drawArc(
            color = Color.White.copy(alpha = 0.25f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
        )
        drawArc(
            color = SkyColors.Sun,
            startAngle = 180f,
            sweepAngle = 180f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
        )
        val angle = PI * (1 - progress)
        val sun = Offset(
            x = size.width / 2 + cos(angle).toFloat() * width / 2,
            y = baseline - sin(angle).toFloat() * height,
        )
        drawCircle(SkyColors.Sun.copy(alpha = 0.13f), 14.dp.toPx(), sun)
        drawCircle(if (isDaylight) SkyColors.Sun else SkyColors.MutedSky, 6.dp.toPx(), sun)
    }
}
