package com.mk.skycast.feature.places.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.SkyHeading
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.core.ui.format.rememberWeatherFormatter
import com.mk.skycast.feature.places.PlacesIntent
import com.mk.skycast.feature.places.PlacesState
import com.mk.skycast.feature.places.R

/** Stateless places screen: search worldwide, or manage saved locations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    state: PlacesState,
    snackbarHostState: SnackbarHostState,
    onIntent: (PlacesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = rememberWeatherFormatter(state.preferences)
    val focusManager = LocalFocusManager.current
    var isEditing by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.places_title), Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(PlacesIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.places_back))
                    }
                },
                actions = {
                    if (!state.isSearchActive && state.savedLocations.isNotEmpty()) {
                        TextButton(onClick = { isEditing = !isEditing }) {
                            Text(stringResource(if (isEditing) R.string.places_done else R.string.places_edit))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = SkySpace.contentMaxWidth).fillMaxWidth()) {
                PlaceSearchField(
                    query = state.query,
                    onQueryChange = { onIntent(PlacesIntent.QueryChanged(it)) },
                    onClear = { onIntent(PlacesIntent.ClearQuery) },
                    // Starts focused only when there is nothing saved yet: searching is the only thing to do.
                    autoFocus = state.savedLocations.isEmpty() && !state.isLocating,
                )
                LazyColumn(
                    contentPadding = PaddingValues(SkySpace.large),
                    verticalArrangement = Arrangement.spacedBy(SkySpace.medium),
                ) {
                    if (state.isSearchActive) {
                        searchResults(state, focusManager, onIntent)
                    } else {
                        savedLocations(state, isEditing, formatter, onIntent)
                    }
                }
            }
        }
    }
}

private fun LazyListScope.searchResults(
    state: PlacesState,
    focusManager: FocusManager,
    onIntent: (PlacesIntent) -> Unit,
) {
    if (state.isSearching) {
        item(key = "search_progress") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
            ) {
                CircularProgressIndicator(Modifier.size(SkyIconSize.medium), strokeWidth = 2.dp)
                Text(
                    stringResource(R.string.places_searching),
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                    },
                )
            }
        }
    }
    state.searchError?.let { error ->
        item(key = "search_error") {
            Text(
                text = error.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
    if (state.showNoResults) {
        item(key = "no_results") {
            PlaceEmpty(stringResource(R.string.places_no_results), stringResource(R.string.places_no_results_body))
        }
    }
    if (state.query.trim().length < PlacesState.MIN_QUERY_LENGTH) {
        item(key = "short_query") {
            Text(stringResource(R.string.places_short_query), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    items(state.suggestions, key = { it.externalId }) { place ->
        SuggestionRow(
            place = place,
            isAdding = place.externalId in state.addingIds,
            onClick = {
                focusManager.clearFocus()
                onIntent(PlacesIntent.SuggestionClicked(place))
            },
            modifier = Modifier.animateItem(),
        )
    }
}

private fun LazyListScope.savedLocations(
    state: PlacesState,
    isEditing: Boolean,
    formatter: WeatherFormatter,
    onIntent: (PlacesIntent) -> Unit,
) {
    if (!state.hasDeviceLocation) {
        item(key = "device") {
            DeviceLocationCard(
                isLocating = state.isLocating,
                onClick = { onIntent(PlacesIntent.UseDeviceLocationClicked) },
            )
        }
    }
    item(key = "saved_heading") { SkyHeading(stringResource(R.string.places_saved)) }
    if (isEditing) {
        item(key = "edit_hint") {
            Text(
                text = stringResource(R.string.places_edit_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    if (state.savedLocations.isEmpty()) {
        item(key = "empty") {
            PlaceEmpty(stringResource(R.string.places_empty), stringResource(R.string.places_empty_body))
        }
    }
    // Only user-added cities are reorderable; "my location" is always pinned first.
    val reorderableIds = state.savedLocations.filterNot { it.location.isDeviceLocation }.map { it.location.id }
    items(state.savedLocations, key = { it.location.id }) { item ->
        SavedPlaceItem(
            item = item,
            isSelected = item.location.id == state.selectedLocationId,
            isEditing = isEditing,
            reorderableIds = reorderableIds,
            now = state.now,
            formatter = formatter,
            onIntent = onIntent,
            modifier = Modifier.animateItem(),
        )
    }
}

/** Moves [id] by [delta] positions and returns the new order, or null if it can't move. */
internal fun List<Long>.moved(id: Long, delta: Int): List<Long>? {
    val from = indexOf(id)
    val to = from + delta
    if (from < 0 || to !in indices) return null
    return toMutableList().apply { add(to, removeAt(from)) }
}
