package com.mk.skycast.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.rememberReducedMotion
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.core.ui.brief.rememberBriefText
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.core.ui.text.UiText
import com.mk.skycast.core.ui.weather.WeatherIcon
import com.mk.skycast.feature.home.HomeIntent
import com.mk.skycast.feature.home.HomeState
import com.mk.skycast.feature.home.R
import com.mk.skycast.feature.home.WeatherPage
import kotlinx.coroutines.flow.drop

/**
 * Pager over saved locations, kept in sync with [HomeState.selectedIndex] in both
 * directions: user swipes send [HomeIntent.PageChanged]; external selection
 * changes (e.g. picking a city in Places) scroll the pager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPager(
    state: HomeState,
    formatter: WeatherFormatter,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = state.selectedIndex.coerceIn(0, state.pages.lastIndex),
        pageCount = { state.pages.size },
    )
    SyncPagerWithSelection(pagerState, state, onIntent)

    Column(modifier) {
        if (state.pages.size > 1) {
            PageIndicator(
                pages = state.pages,
                selectedIndex = state.selectedIndex,
                formatter = formatter,
                onSelect = { onIntent(HomeIntent.PageChanged(it)) },
            )
        }
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(HomeIntent.Refresh) },
            modifier = Modifier.weight(1f),
        ) {
            HorizontalPager(
                state = pagerState,
                key = { state.pages[it].location.id },
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
            ) { index ->
                val page = state.pages[index]
                val error = state.pageErrors[page.location.id]
                when {
                    page.weather != null -> WeatherPageContent(
                        page = page,
                        now = state.now,
                        formatter = formatter,
                        brief = briefSlot(state, page, formatter, onIntent),
                        ask = askSlot(state, page, onIntent),
                    )

                    error != null -> PageError(error, onRetry = { onIntent(HomeIntent.Refresh) })

                    else -> WeatherSkeleton(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun SyncPagerWithSelection(pagerState: PagerState, state: HomeState, onIntent: (HomeIntent) -> Unit) {
    val reducedMotion = rememberReducedMotion()
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    val latestOnIntent by rememberUpdatedState(onIntent)
    val latestSelectedIndex by rememberUpdatedState(state.selectedIndex)

    LaunchedEffect(state.selectedIndex, state.pages.size) {
        val target = state.selectedIndex.coerceIn(0, state.pages.lastIndex)
        if (pagerState.currentPage == target) return@LaunchedEffect
        isProgrammaticScroll = true
        try {
            if (reducedMotion) pagerState.scrollToPage(target) else pagerState.animateScrollToPage(target)
        } finally {
            isProgrammaticScroll = false
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            if (!isProgrammaticScroll && page != latestSelectedIndex) latestOnIntent(HomeIntent.PageChanged(page))
        }
    }
}

@Composable
private fun PageIndicator(
    pages: List<WeatherPage>,
    selectedIndex: Int,
    formatter: WeatherFormatter,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        pages.forEachIndexed { index, page ->
            val selected = index == selectedIndex
            val description = stringResource(
                R.string.home_page,
                formatter.number(index + 1),
                formatter.number(pages.size),
            )
            Box(
                modifier = Modifier
                    .size(SkySpace.touch)
                    .clip(CircleShape)
                    .clickable { onSelect(index) }
                    .semantics {
                        contentDescription = description
                        this.selected = selected
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (page.location.isDeviceLocation) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (selected) Color.White else SkyColors.MutedSky.copy(alpha = 0.5f),
                    )
                } else {
                    Box(
                        Modifier
                            .size(if (selected) 8.dp else 6.dp)
                            .background(Color.White.copy(alpha = if (selected) 1f else 0.4f), CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageError(error: UiText, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SkySpace.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WeatherIcon(WeatherCondition.OVERCAST, isDay = true, modifier = Modifier.size(112.dp), animated = false)
        Text(error.asString(), textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = SkySpace.large)) {
            Text(stringResource(R.string.home_retry))
        }
    }
}

@Composable
private fun askSlot(state: HomeState, page: WeatherPage, onIntent: (HomeIntent) -> Unit): (@Composable () -> Unit)? {
    if (page.weather == null || page.location.id != state.briefLocationId) return null
    return {
        AskEntryRow(locationName = page.location.name, onClick = { onIntent(HomeIntent.AskOpened(page.location.id)) })
    }
}

/** The brief (or the routine prompt) lives on the page of the routine's location only. */
private fun briefSlot(
    state: HomeState,
    page: WeatherPage,
    formatter: WeatherFormatter,
    onIntent: (HomeIntent) -> Unit,
): (@Composable () -> Unit)? {
    if (page.location.id != state.briefLocationId) return null
    val brief = state.brief
    return when {
        state.showRoutinePrompt -> {
            { RoutinePromptCard(onSetUp = { onIntent(HomeIntent.OpenRoutineClicked) }) }
        }

        brief != null && page.weather != null -> {
            {
                BriefCard(
                    brief = brief,
                    text = rememberBriefText(formatter, page.weather.zoneId),
                    now = state.now,
                    expanded = state.isBriefExpanded,
                    hasPlanChange = state.overrides.any { it.date == brief.date },
                    onToggleExpand = { onIntent(HomeIntent.BriefExpandToggled) },
                    onChangePlans = { onIntent(HomeIntent.PlansChangedClicked) },
                    onEditRoutine = { onIntent(HomeIntent.OpenRoutineClicked) },
                )
            }
        }

        else -> null
    }
}
