package com.mk.skycast.feature.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.GlassCard
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.DailyForecast
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.core.ui.weather.WeatherIcon
import com.mk.skycast.feature.home.R
import com.mk.skycast.feature.home.WeatherPage

private const val SHOW_PRECIPITATION_FROM_PERCENT = 20

/** Above this font scale each day wraps onto two lines instead of truncating. */
private const val LARGE_TEXT_FONT_SCALE = 1.25f

/** Day rows with min/max range bars on a shared scale across the whole forecast. */
@Composable
internal fun DailyCard(page: WeatherPage, formatter: WeatherFormatter, modifier: Modifier = Modifier) {
    val days = page.days
    val scale = remember(days) {
        TemperatureScale(days.minOf { it.temperatureMinC }, days.maxOf { it.temperatureMaxC })
    }
    GlassCard(modifier.fillMaxWidth()) {
        CardTitle(Icons.Rounded.CalendarMonth, stringResource(R.string.home_daily))
        days.forEachIndexed { index, day ->
            key(day.date) {
                val isToday = index == 0
                DailyRow(
                    day = day,
                    label = if (isToday) stringResource(R.string.home_today) else formatter.dayOfWeekShort(day.date),
                    scale = scale,
                    currentTemperature = if (isToday) page.weather?.current?.temperatureC else null,
                    formatter = formatter,
                )
                if (index != days.lastIndex) HorizontalDivider(color = SkyColors.Divider)
            }
        }
    }
}

private data class TemperatureScale(val min: Double, val max: Double)

@Composable
private fun DailyRow(
    day: DailyForecast,
    label: String,
    scale: TemperatureScale,
    currentTemperature: Double?,
    formatter: WeatherFormatter,
) {
    val probability = day.precipitationProbabilityMax?.takeIf { it >= SHOW_PRECIPITATION_FROM_PERCENT }
    val range = @Composable { rangeModifier: Modifier ->
        TemperatureRange(day.temperatureMinC, day.temperatureMaxC, scale, currentTemperature, rangeModifier)
    }
    if (LocalDensity.current.fontScale > LARGE_TEXT_FONT_SCALE) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(SkySpace.small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                WeatherIcon(day.condition, isDay = true, modifier = Modifier.size(32.dp), animated = false)
                probability?.let {
                    Text(
                        text = formatter.percent(it),
                        modifier = Modifier.padding(start = SkySpace.small),
                        color = SkyColors.Ice,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkySpace.small),
            ) {
                Text(formatter.temperature(day.temperatureMinC), color = SkyColors.MutedSky)
                range(Modifier.weight(1f).height(18.dp))
                Text(formatter.temperature(day.temperatureMaxC))
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(label, Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium)
            Column(Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                WeatherIcon(day.condition, isDay = true, modifier = Modifier.size(28.dp), animated = false)
                probability?.let {
                    Text(formatter.percent(it), color = SkyColors.Ice, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                text = formatter.temperature(day.temperatureMinC),
                modifier = Modifier.widthIn(min = 30.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = SkyColors.MutedSky,
            )
            range(Modifier.weight(1f).height(18.dp))
            Text(
                text = formatter.temperature(day.temperatureMaxC),
                modifier = Modifier.widthIn(min = 30.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Cold → warm bar for [low]..[high], positioned on the shared [scale], with an optional "now" dot. */
@Composable
private fun TemperatureRange(
    low: Double,
    high: Double,
    scale: TemperatureScale,
    current: Double?,
    modifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val gradient = remember(rtl) {
        Brush.horizontalGradient(
            if (rtl) SkyColors.TemperatureRange.reversed() else SkyColors.TemperatureRange,
        )
    }
    Canvas(modifier) {
        val inset = 4.dp.toPx()
        val width = (size.width - inset * 2).coerceAtLeast(1f)
        val span = (scale.max - scale.min).coerceAtLeast(1.0)
        fun position(value: Double): Float {
            val fraction = ((value - scale.min) / span).coerceIn(0.0, 1.0).toFloat()
            return inset + width * if (rtl) 1 - fraction else fraction
        }
        val y = size.height / 2
        val stroke = 5.dp.toPx()
        drawLine(SkyColors.Track, Offset(inset, y), Offset(size.width - inset, y), stroke, StrokeCap.Round)
        drawLine(gradient, Offset(position(low), y), Offset(position(high), y), stroke, StrokeCap.Round)
        if (current != null) {
            val center = Offset(position(current), y)
            drawCircle(SkyColors.Ink, 4.dp.toPx(), center)
            drawCircle(Color.White, 2.5.dp.toPx(), center)
        }
    }
}
