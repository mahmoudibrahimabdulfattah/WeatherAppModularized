package com.mk.skycast.core.ui.weather

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mk.skycast.core.designsystem.components.SkyCondition
import com.mk.skycast.core.designsystem.components.WeatherBackdrop as SkyBackdrop
import com.mk.skycast.core.designsystem.components.WeatherIcon as SkyIcon
import com.mk.skycast.core.model.WeatherCondition

/** Maps the domain condition onto the design system's rendering vocabulary. */
internal fun WeatherCondition.toSkyCondition(): SkyCondition = when (this) {
    WeatherCondition.CLEAR -> SkyCondition.Clear

    WeatherCondition.MAINLY_CLEAR,
    WeatherCondition.PARTLY_CLOUDY,
    -> SkyCondition.Partial

    WeatherCondition.OVERCAST -> SkyCondition.Cloud

    WeatherCondition.FOG -> SkyCondition.Fog

    WeatherCondition.DRIZZLE,
    WeatherCondition.FREEZING_DRIZZLE,
    WeatherCondition.RAIN,
    WeatherCondition.FREEZING_RAIN,
    WeatherCondition.RAIN_SHOWERS,
    -> SkyCondition.Rain

    WeatherCondition.SNOW,
    WeatherCondition.SNOW_GRAINS,
    WeatherCondition.SNOW_SHOWERS,
    -> SkyCondition.Snow

    WeatherCondition.THUNDERSTORM,
    WeatherCondition.THUNDERSTORM_HAIL,
    -> SkyCondition.Thunder

    WeatherCondition.UNKNOWN -> SkyCondition.Unknown
}

@Composable
fun WeatherBackdrop(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    SkyBackdrop(condition.toSkyCondition(), isDay, modifier, animated)
}

/**
 * Weather icon with an accessible label. Pass [decorative] = true when the
 * condition is already announced by nearby text.
 */
@Composable
fun WeatherIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    decorative: Boolean = false,
) {
    val description = conditionLabel(condition)
    val semanticsModifier = if (decorative) modifier else modifier.semantics { contentDescription = description }
    SkyIcon(condition.toSkyCondition(), isDay, semanticsModifier, animated)
}
