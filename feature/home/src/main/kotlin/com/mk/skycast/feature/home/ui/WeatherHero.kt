package com.mk.skycast.feature.home.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.mk.skycast.core.designsystem.components.rememberReducedMotion
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.core.ui.weather.WeatherIcon
import com.mk.skycast.core.ui.weather.conditionLabel
import com.mk.skycast.feature.home.R
import com.mk.skycast.feature.home.WeatherPage
import java.time.Duration
import java.time.Instant

private const val HERO_MAX_FONT_SP = 120f

/** Approximate glyph width relative to font size, used to fit the temperature on one line. */
private const val HERO_GLYPH_WIDTH_RATIO = 0.58f
private const val FADE_IN_MS = 350
private const val FADE_OUT_MS = 250

/** Icon, big temperature, condition, feels-like, high/low, local time and a live "updated" label. */
@Composable
internal fun WeatherHero(page: WeatherPage, now: Instant, formatter: WeatherFormatter, modifier: Modifier = Modifier) {
    val weather = page.weather ?: return
    val current = weather.current
    val minutesSinceUpdate = Duration.between(weather.fetchedAt, now).toMinutes().coerceAtLeast(0).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SkySpace.medium, bottom = SkySpace.large)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WeatherIcon(current.condition, current.isDay, Modifier.size(SkyIconSize.hero), decorative = true)
        HeroTemperature(formatter.temperature(current.temperatureC))
        Text(conditionLabel(current.condition), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(R.string.home_feels, formatter.temperature(current.apparentTemperatureC)),
            modifier = Modifier.padding(top = SkySpace.small),
            color = SkyColors.MutedSky,
        )
        page.today?.let { today ->
            Text(
                text = stringResource(
                    R.string.home_high_low,
                    formatter.temperature(today.temperatureMaxC),
                    formatter.temperature(today.temperatureMinC),
                ),
                modifier = Modifier.padding(top = SkySpace.tiny),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(SkySpace.large))
        Text(
            text = stringResource(R.string.home_local_time, formatter.time(now, weather.zoneId)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = if (minutesSinceUpdate < 1) {
                stringResource(R.string.home_just_now)
            } else {
                stringResource(R.string.home_updated, formatter.number(minutesSinceUpdate))
            },
            modifier = Modifier.padding(top = SkySpace.tiny),
            style = MaterialTheme.typography.labelSmall,
            color = SkyColors.MutedSky,
        )
        if (page.isStale) {
            Text(
                text = stringResource(R.string.home_stale),
                style = MaterialTheme.typography.labelSmall,
                color = SkyColors.Sun,
            )
        }
    }
}

/** Largest font that keeps the temperature on one line, honoring the user's font scale. */
@Composable
private fun HeroTemperature(temperature: String, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fitted = maxWidth.value / (temperature.length * HERO_GLYPH_WIDTH_RATIO * fontScale)
        val fontSize = minOf(HERO_MAX_FONT_SP, fitted).sp
        AnimatedContent(
            targetState = temperature,
            transitionSpec = {
                fadeIn(tween(if (reducedMotion) 0 else FADE_IN_MS)) togetherWith
                    fadeOut(tween(if (reducedMotion) 0 else FADE_OUT_MS))
            },
            label = "temperature",
        ) { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                fontSize = fontSize,
                lineHeight = fontSize * 1.1f,
            )
        }
    }
}
