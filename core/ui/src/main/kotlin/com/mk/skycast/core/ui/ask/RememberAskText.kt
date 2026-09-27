package com.mk.skycast.core.ui.ask

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mk.skycast.core.ui.format.WeatherFormatter
import java.time.ZoneId

@Composable
fun rememberAskText(formatter: WeatherFormatter, zone: ZoneId): AskText {
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current
    return remember(resources, configuration, formatter, zone) { AskText(resources, formatter, zone) }
}
