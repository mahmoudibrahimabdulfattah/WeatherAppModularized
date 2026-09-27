package com.mk.skycast.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.feature.home.R

/** Search · location title · settings. */
@Composable
internal fun HomeHeader(
    location: SavedLocation?,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(SkySpace.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.home_search))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkySpace.tiny),
            ) {
                if (location?.isDeviceLocation == true) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription = stringResource(R.string.home_device),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = location?.name ?: stringResource(R.string.home_brand),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
            val subtitle = location?.let { listOfNotNull(it.region, it.country).distinct().joinToString(" · ") }
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyColors.MutedSky,
                    textAlign = TextAlign.Center,
                )
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.home_settings))
        }
    }
}
