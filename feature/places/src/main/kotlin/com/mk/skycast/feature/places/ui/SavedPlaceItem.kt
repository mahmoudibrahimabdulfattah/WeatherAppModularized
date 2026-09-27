package com.mk.skycast.feature.places.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.LocationWeather
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.core.ui.weather.WeatherBackdrop
import com.mk.skycast.core.ui.weather.WeatherIcon
import com.mk.skycast.core.ui.weather.conditionLabel
import com.mk.skycast.feature.places.PlacesIntent
import com.mk.skycast.feature.places.R
import java.time.Instant

/** Saved city card with swipe-to-remove, plus move/remove controls in edit mode. */
@Composable
internal fun SavedPlaceItem(
    item: LocationWeather,
    isSelected: Boolean,
    isEditing: Boolean,
    reorderableIds: List<Long>,
    now: Instant,
    formatter: WeatherFormatter,
    onIntent: (PlacesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val location = item.location
    val remove = { onIntent(PlacesIntent.RemoveLocation(location.id)) }
    val removeLabel = stringResource(R.string.places_remove, location.name)
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            remove()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = { RemoveBackground(removeLabel) },
    ) {
        Column {
            SavedPlaceCard(
                item = item,
                isSelected = isSelected,
                now = now,
                formatter = formatter,
                onClick = { onIntent(PlacesIntent.LocationClicked(location.id)) },
                modifier = Modifier.semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(removeLabel) {
                            remove()
                            true
                        },
                    )
                },
            )
            if (isEditing) {
                EditControls(
                    name = location.name,
                    canReorder = !location.isDeviceLocation,
                    newOrderUp = reorderableIds.moved(location.id, -1),
                    newOrderDown = reorderableIds.moved(location.id, 1),
                    removeLabel = removeLabel,
                    onReorder = { onIntent(PlacesIntent.ReorderLocations(it)) },
                    onRemove = remove,
                )
            }
        }
    }
}

@Composable
private fun RemoveBackground(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(SkySpace.large),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = label, tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun EditControls(
    name: String,
    canReorder: Boolean,
    newOrderUp: List<Long>?,
    newOrderDown: List<Long>?,
    removeLabel: String,
    onReorder: (List<Long>) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canReorder) {
            IconButton(onClick = { newOrderUp?.let(onReorder) }, enabled = newOrderUp != null) {
                Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.places_move_up, name))
            }
            IconButton(onClick = { newOrderDown?.let(onReorder) }, enabled = newOrderDown != null) {
                Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.places_move_down, name))
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.DeleteOutline, removeLabel, tint = MaterialTheme.colorScheme.error)
        }
    }
}

/** Mini sky of the city's current condition with its local time, condition and temperatures. */
@Composable
private fun SavedPlaceCard(
    item: LocationWeather,
    isSelected: Boolean,
    now: Instant,
    formatter: WeatherFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weather = item.weather
    val current = weather?.current
    val shape = MaterialTheme.shapes.extraLarge
    val selectedLabel = stringResource(R.string.places_selected)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (isSelected) Modifier.border(2.dp, SkyColors.Ice, shape) else Modifier)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                selected = isSelected
                if (isSelected) stateDescription = selectedLabel
            },
    ) {
        WeatherBackdrop(
            condition = current?.condition ?: WeatherCondition.CLEAR,
            isDay = current?.isDay ?: false,
            modifier = Modifier.matchParentSize(),
            animated = false,
        )
        CompositionLocalProvider(LocalContentColor provides SkyColors.OnSky) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SkySpace.large),
                horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SkySpace.tiny)) {
                    if (item.location.isDeviceLocation) {
                        Text(stringResource(R.string.places_device), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(item.location.name, style = MaterialTheme.typography.headlineSmall)
                    weather?.let {
                        Text(
                            text = formatter.time(now, it.zoneId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyColors.MutedSky,
                        )
                    }
                    Spacer(Modifier.height(SkySpace.small))
                    Text(
                        text = current?.let { conditionLabel(it.condition) } ?: stringResource(R.string.places_loading),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val today = weather?.daily?.firstOrNull { it.date == now.atZone(weather.zoneId).toLocalDate() }
                    if (today != null) {
                        Text(
                            text = stringResource(
                                R.string.places_high_low,
                                formatter.temperature(today.temperatureMaxC),
                                formatter.temperature(today.temperatureMinC),
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (current != null) {
                        Text(
                            formatter.temperature(current.temperatureC),
                            style = MaterialTheme.typography.displayMedium,
                        )
                        WeatherIcon(
                            current.condition,
                            current.isDay,
                            Modifier.size(52.dp),
                            animated = false,
                            decorative = true,
                        )
                    } else {
                        Icon(Icons.Rounded.CloudQueue, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = SkyColors.Ice,
                        )
                    }
                }
            }
        }
    }
}
