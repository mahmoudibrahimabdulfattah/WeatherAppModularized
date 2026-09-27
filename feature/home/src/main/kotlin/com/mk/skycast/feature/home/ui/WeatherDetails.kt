package com.mk.skycast.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.GlassCard
import com.mk.skycast.core.designsystem.components.SkyHeading
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.AirQuality
import com.mk.skycast.core.model.CurrentWeather
import com.mk.skycast.core.ui.R as CoreUiR
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.core.ui.weather.airQualityLabel
import com.mk.skycast.core.ui.weather.conditionLabel
import com.mk.skycast.core.ui.weather.uvLabel
import com.mk.skycast.feature.home.R
import com.mk.skycast.feature.home.WeatherPage
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Scale maxima for the meters. */
private const val UV_SCALE_MAX = 12.0
private const val AQI_SCALE_MAX = 500f
private const val PERCENT_MAX = 100f

/** "A closer look": grid of metric tiles, air quality and the sun arc. */
@Composable
internal fun WeatherDetails(
    page: WeatherPage,
    now: Instant,
    formatter: WeatherFormatter,
    modifier: Modifier = Modifier,
) {
    val weather = page.weather ?: return
    val current = weather.current
    val unavailable = stringResource(CoreUiR.string.core_ui_unavailable)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.medium)) {
        SkyHeading(
            text = stringResource(R.string.home_details),
            modifier = Modifier.padding(start = SkySpace.small, top = SkySpace.small),
        )
        TileRow(
            start = { FeelsLikeTile(current, formatter, it) },
            end = { HumidityTile(current, formatter, it) },
        )
        TileRow(
            start = { WindTile(current, formatter, it) },
            end = { UvTile(current, formatter, unavailable, it) },
        )
        TileRow(
            start = {
                DetailTile(
                    stringResource(R.string.home_pressure),
                    formatter.pressure(current.pressureHpa),
                    Icons.Rounded.Speed,
                    it,
                ) {
                    SupportingText(stringResource(R.string.home_current))
                }
            },
            end = {
                val visibility = current.visibilityMeters?.let(formatter::visibility) ?: unavailable
                DetailTile(stringResource(R.string.home_visibility), visibility, Icons.Rounded.Visibility, it) {
                    SupportingText(stringResource(R.string.home_current))
                }
            },
        )
        TileRow(
            start = { PrecipitationTile(page, current, formatter, it) },
            end = { CloudCoverTile(current, formatter, it) },
        )
        weather.airQuality?.let { AirQualityTile(it, formatter, unavailable) }
        SunCard(page, now, weather.zoneId, formatter, unavailable)
    }
}

@Composable
private fun FeelsLikeTile(current: CurrentWeather, formatter: WeatherFormatter, modifier: Modifier = Modifier) {
    DetailTile(
        title = stringResource(R.string.home_feels_title),
        value = formatter.temperature(current.apparentTemperatureC),
        icon = Icons.Rounded.Thermostat,
        modifier = modifier,
    ) {
        SupportingText(conditionLabel(current.condition))
    }
}

@Composable
private fun HumidityTile(current: CurrentWeather, formatter: WeatherFormatter, modifier: Modifier = Modifier) {
    DetailTile(
        title = stringResource(R.string.home_humidity),
        value = formatter.percent(current.relativeHumidity),
        icon = Icons.Rounded.WaterDrop,
        modifier = modifier,
    ) {
        current.dewPointC?.let { SupportingText(stringResource(R.string.home_dew, formatter.temperature(it))) }
        ScaleMeter(current.relativeHumidity / PERCENT_MAX, listOf(SkyColors.Ice, SkyColors.DeepIce))
    }
}

@Composable
private fun WindTile(current: CurrentWeather, formatter: WeatherFormatter, modifier: Modifier = Modifier) {
    DetailTile(
        title = stringResource(R.string.home_wind),
        value = formatter.windSpeed(current.windSpeedKmh),
        icon = Icons.Rounded.Air,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkySpace.small),
        ) {
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = null,
                modifier = Modifier.size(28.dp).rotate(current.windDirectionDegrees.toFloat()),
                tint = SkyColors.Ice,
            )
            Text(
                text = stringResource(
                    R.string.home_wind_direction,
                    formatter.windDirection(current.windDirectionDegrees),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        current.windGustsKmh?.let { SupportingText(stringResource(R.string.home_gusts, formatter.windSpeed(it))) }
    }
}

@Composable
private fun PrecipitationTile(
    page: WeatherPage,
    current: CurrentWeather,
    formatter: WeatherFormatter,
    modifier: Modifier = Modifier,
) {
    val today = page.today
    DetailTile(
        title = stringResource(R.string.home_precipitation),
        value = formatter.precipitation(today?.precipitationSumMm ?: current.precipitationMm),
        icon = Icons.Rounded.Umbrella,
        modifier = modifier,
    ) {
        SupportingText(stringResource(if (today != null) R.string.home_today_total else R.string.home_current))
    }
}

@Composable
private fun CloudCoverTile(current: CurrentWeather, formatter: WeatherFormatter, modifier: Modifier = Modifier) {
    DetailTile(
        title = stringResource(R.string.home_clouds),
        value = formatter.percent(current.cloudCover),
        icon = Icons.Rounded.CloudQueue,
        modifier = modifier,
    ) {
        ScaleMeter(current.cloudCover / PERCENT_MAX, listOf(SkyColors.Ice, Color.White))
        SupportingText(conditionLabel(current.condition))
    }
}

@Composable
private fun UvTile(
    current: CurrentWeather,
    formatter: WeatherFormatter,
    unavailable: String,
    modifier: Modifier = Modifier,
) {
    val uv = current.uvIndex
    DetailTile(
        title = stringResource(R.string.home_uv),
        value = uv?.let(formatter::uvIndex) ?: unavailable,
        icon = Icons.Rounded.WbSunny,
        modifier = modifier,
    ) {
        if (uv != null) {
            SupportingText(uvLabel(uv), color = SkyColors.OnSky)
            ScaleMeter((uv / UV_SCALE_MAX).toFloat(), SkyColors.Severity)
        }
    }
}

@Composable
private fun AirQualityTile(air: AirQuality, formatter: WeatherFormatter, unavailable: String) {
    val aqi = air.usAqi
    DetailTile(
        title = stringResource(R.string.home_air),
        value = aqi?.let { stringResource(R.string.home_aqi, formatter.number(it)) } ?: unavailable,
        icon = Icons.Rounded.Spa,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SupportingText(airQualityLabel(air.level), color = SkyColors.OnSky)
        aqi?.let { ScaleMeter(it / AQI_SCALE_MAX, SkyColors.Severity) }
    }
}

@Composable
private fun SunCard(page: WeatherPage, now: Instant, zone: ZoneId, formatter: WeatherFormatter, unavailable: String) {
    val sunrise = page.today?.sunrise
    val sunset = page.today?.sunset
    GlassCard(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        CardTitle(Icons.Rounded.WbTwilight, stringResource(R.string.home_sun), tint = SkyColors.Sun)
        if (sunrise != null && sunset != null && sunset > sunrise) {
            val progress = remember(now, sunrise, sunset) { dayProgress(now, sunrise, sunset) }
            SunArc(progress, isDaylight = now in sunrise..sunset)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SkySpace.medium)) {
            Text(
                text = stringResource(R.string.home_sunrise, sunrise?.let { formatter.time(it, zone) } ?: unavailable),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.home_sunset, sunset?.let { formatter.time(it, zone) } ?: unavailable),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
            )
        }
    }
}

private fun dayProgress(now: Instant, sunrise: Instant, sunset: Instant): Float {
    val elapsed = Duration.between(sunrise, now).toMillis().toDouble()
    val length = Duration.between(sunrise, sunset).toMillis()
    return (elapsed / length).coerceIn(0.0, 1.0).toFloat()
}
