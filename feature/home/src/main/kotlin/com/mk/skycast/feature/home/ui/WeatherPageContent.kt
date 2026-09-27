package com.mk.skycast.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mk.skycast.core.designsystem.components.SkyEntrance
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.feature.home.WeatherPage
import java.time.Instant

private const val HERO_PANE_WEIGHT = 0.85f
private const val CARDS_PANE_WEIGHT = 1.15f

/** One location's weather: single column on phones, hero + cards side by side on wide screens. */
@Composable
internal fun WeatherPageContent(
    page: WeatherPage,
    now: Instant,
    formatter: WeatherFormatter,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= SkySpace.paneBreakpoint) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = SkySpace.large),
                horizontalArrangement = Arrangement.spacedBy(SkySpace.large),
            ) {
                Column(
                    modifier = Modifier.weight(HERO_PANE_WEIGHT).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WeatherHero(page, now, formatter)
                }
                Column(
                    modifier = Modifier
                        .weight(CARDS_PANE_WEIGHT)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = SkySpace.large),
                    verticalArrangement = Arrangement.spacedBy(SkySpace.medium),
                ) {
                    ForecastSections(page, now, formatter)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SkySpace.medium)
                    .padding(bottom = SkySpace.large),
                verticalArrangement = Arrangement.spacedBy(SkySpace.medium),
            ) {
                WeatherHero(page, now, formatter)
                ForecastSections(page, now, formatter)
            }
        }
    }
}

@Composable
private fun ForecastSections(page: WeatherPage, now: Instant, formatter: WeatherFormatter) {
    if (page.upcomingHours.isNotEmpty()) SkyEntrance(index = 0) { HourlyCard(page, formatter) }
    if (page.days.isNotEmpty()) SkyEntrance(index = 1) { DailyCard(page, formatter) }
    SkyEntrance(index = 2) { WeatherDetails(page, now, formatter) }
}
