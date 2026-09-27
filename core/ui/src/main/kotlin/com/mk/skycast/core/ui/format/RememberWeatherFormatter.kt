package com.mk.skycast.core.ui.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mk.skycast.core.model.UserPreferences

/** A [WeatherFormatter] bound to the current locale, 12/24h system setting and [preferences]. */
@Composable
fun rememberWeatherFormatter(preferences: UserPreferences): WeatherFormatter {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return remember(preferences, locale, is24Hour) { WeatherFormatter(preferences, locale, is24Hour) }
}
