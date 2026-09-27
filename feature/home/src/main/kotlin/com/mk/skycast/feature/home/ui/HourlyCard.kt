package com.mk.skycast.feature.home.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.GlassCard
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.HourlyForecast
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.core.ui.weather.WeatherIcon
import com.mk.skycast.feature.home.R
import com.mk.skycast.feature.home.WeatherPage
import java.time.ZoneId

private const val HOUR_COLUMN_WIDTH_DP = 76
private const val SHOW_PRECIPITATION_FROM_PERCENT = 20
private const val CURVE_PADDING_DEGREES = 2.0

/** Next 24 hours with a smooth temperature curve under the columns. */
@Composable
internal fun HourlyCard(page: WeatherPage, formatter: WeatherFormatter, modifier: Modifier = Modifier) {
    val weather = page.weather ?: return
    val hours = page.upcomingHours
    val temperatures = remember(hours) { hours.map { it.temperatureC } }
    // Columns grow with the font scale so labels never truncate.
    val columnWidth = (HOUR_COLUMN_WIDTH_DP * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp

    GlassCard(modifier.fillMaxWidth()) {
        CardTitle(Icons.Rounded.Schedule, stringResource(R.string.home_hourly))
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row {
                hours.forEachIndexed { index, hour ->
                    key(hour.time) {
                        HourColumn(
                            hour,
                            isNow = index == 0,
                            zone = weather.zoneId,
                            formatter,
                            Modifier.width(columnWidth),
                        )
                    }
                }
            }
            TemperatureCurve(temperatures, width = columnWidth * hours.size)
        }
    }
}

@Composable
private fun HourColumn(
    hour: HourlyForecast,
    isNow: Boolean,
    zone: ZoneId,
    formatter: WeatherFormatter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkySpace.small),
    ) {
        Text(
            text = if (isNow) stringResource(R.string.home_now) else formatter.hour(hour.time, zone),
            style = MaterialTheme.typography.labelLarge,
        )
        WeatherIcon(hour.condition, hour.isDay, Modifier.size(SkyIconSize.large), animated = false)
        Text(formatter.temperature(hour.temperatureC), style = MaterialTheme.typography.titleMedium)
        val probability = hour.precipitationProbability
        Text(
            text = if (probability != null && probability >= SHOW_PRECIPITATION_FROM_PERCENT) {
                formatter.percent(probability)
            } else {
                ""
            },
            color = SkyColors.Ice,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun TemperatureCurve(temperatures: List<Double>, width: Dp, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Spacer(
        modifier.width(width).height(48.dp).drawWithCache {
            val path = Path()
            if (temperatures.isNotEmpty()) {
                val min = temperatures.min() - CURVE_PADDING_DEGREES
                val span = (temperatures.max() + CURVE_PADDING_DEGREES - min).coerceAtLeast(1.0)
                val step = size.width / temperatures.size
                fun x(i: Int) = step * (i + 0.5f).let { if (rtl) temperatures.size - it else it }
                fun y(i: Int) = (size.height * (1 - (temperatures[i] - min) / span)).toFloat()
                path.moveTo(x(0), y(0))
                for (i in 1 until temperatures.size) {
                    val midX = (x(i - 1) + x(i)) / 2
                    path.cubicTo(midX, y(i - 1), midX, y(i), x(i), y(i))
                }
            }
            val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            onDrawBehind { drawPath(path, SkyColors.Sun.copy(alpha = 0.9f), style = stroke) }
        },
    )
}
