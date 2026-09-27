package com.mk.skycast.feature.places.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.feature.places.R
import java.util.Locale

/** One search result: flag, name, region · country, and an add action. */
@Composable
internal fun SuggestionRow(
    place: PlaceSuggestion,
    isAdding: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flag = remember(place.countryCode) { flagEmoji(place.countryCode) }
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isAdding, onClick = onClick)
                .padding(SkySpace.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
        ) {
            Text(flag, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clearAndSetSemantics {})
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = listOfNotNull(place.region, place.country).distinct().joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (isAdding) {
                CircularProgressIndicator(Modifier.size(SkyIconSize.medium), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.places_add, place.name))
            }
        }
    }
}

/** Call to action for adding the device location. */
@Composable
internal fun DeviceLocationCard(isLocating: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = !isLocating,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SkySpace.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
        ) {
            if (isLocating) {
                CircularProgressIndicator(Modifier.size(SkyIconSize.medium), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.MyLocation, contentDescription = null)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(if (isLocating) R.string.places_locating else R.string.places_use_location),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.places_location_hint), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
internal fun PlaceEmpty(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = SkySpace.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkySpace.medium),
    ) {
        Icon(
            imageVector = Icons.Rounded.TravelExplore,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val REGIONAL_INDICATOR_A = 0x1F1E6

/** "EG" → 🇪🇬 using Unicode regional indicator symbols. */
internal fun flagEmoji(countryCode: String?): String {
    val code = countryCode?.uppercase(Locale.ROOT) ?: return ""
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return ""
    return code.map { String(Character.toChars(REGIONAL_INDICATOR_A + (it - 'A'))) }.joinToString("")
}
