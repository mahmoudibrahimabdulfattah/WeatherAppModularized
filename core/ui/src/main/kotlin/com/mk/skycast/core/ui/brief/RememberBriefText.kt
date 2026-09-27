package com.mk.skycast.core.ui.brief

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mk.skycast.core.ui.format.WeatherFormatter
import java.time.ZoneId

/** [BriefText] bound to the current (possibly per-app) locale's resources. */
@Composable
fun rememberBriefText(formatter: WeatherFormatter, zone: ZoneId): BriefText {
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current
    return remember(resources, configuration, formatter, zone) { BriefText(resources, formatter, zone) }
}
