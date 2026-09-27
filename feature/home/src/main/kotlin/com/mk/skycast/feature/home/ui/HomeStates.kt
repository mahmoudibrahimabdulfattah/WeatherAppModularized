package com.mk.skycast.feature.home.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.GlassCard
import com.mk.skycast.core.designsystem.components.rememberReducedMotion
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.WeatherCondition
import com.mk.skycast.core.ui.weather.WeatherIcon
import com.mk.skycast.feature.home.R

private const val SHIMMER_MIN_ALPHA = 0.08f
private const val SHIMMER_MAX_ALPHA = 0.2f
private const val SHIMMER_STATIC_ALPHA = 0.16f
private const val SHIMMER_CYCLE_MS = 1100
private const val SKELETON_CARDS = 3

/** First-launch state: explains the app and offers location or search. */
@Composable
internal fun HomeWelcome(
    isLocating: Boolean,
    onUseLocation: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(SkySpace.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WeatherIcon(WeatherCondition.PARTLY_CLOUDY, isDay = true, modifier = Modifier.size(SkyIconSize.illustration))
        Spacer(Modifier.height(SkySpace.extraLarge))
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_empty_body),
            modifier = Modifier.padding(vertical = SkySpace.large),
            textAlign = TextAlign.Center,
            color = SkyColors.MutedSky,
        )
        Button(
            onClick = onUseLocation,
            enabled = !isLocating,
            modifier = Modifier.heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SkyColors.Sun, contentColor = SkyColors.Ink),
        ) {
            if (isLocating) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SkyColors.Ink)
            } else {
                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(SkySpace.small))
            Text(stringResource(if (isLocating) R.string.home_locating else R.string.home_use_location))
        }
        TextButton(
            onClick = onSearch,
            modifier = Modifier.padding(top = SkySpace.small),
            colors = ButtonDefaults.textButtonColors(contentColor = SkyColors.OnSky),
        ) {
            Text(stringResource(R.string.home_search))
        }
    }
}

@Composable
internal fun OfflineBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SkyColors.Banner)
            .padding(horizontal = SkySpace.large, vertical = SkySpace.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkySpace.small),
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(SkyIconSize.small))
        Text(
            text = stringResource(R.string.home_offline),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Pulsing placeholder shaped like the hero and cards. */
@Composable
internal fun WeatherSkeleton(modifier: Modifier = Modifier) {
    val alpha = rememberShimmerAlpha()
    val label = stringResource(R.string.home_loading)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(SkySpace.large)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SkySpace.large),
    ) {
        val placeholder = Color.White.copy(alpha = alpha.value)
        Spacer(Modifier.height(SkySpace.large))
        Box(Modifier.size(88.dp).background(placeholder, CircleShape))
        Box(Modifier.size(160.dp, 112.dp).clip(MaterialTheme.shapes.extraLarge).background(placeholder))
        Text(label, color = SkyColors.MutedSky, style = MaterialTheme.typography.bodyMedium)
        repeat(SKELETON_CARDS) {
            GlassCard(Modifier.fillMaxWidth().height(152.dp)) {
                Box(Modifier.fillMaxWidth(0.45f).height(16.dp).background(placeholder, CircleShape))
                Box(Modifier.fillMaxWidth().height(56.dp).background(placeholder, MaterialTheme.shapes.large))
            }
        }
    }
}

@Composable
private fun rememberShimmerAlpha(): State<Float> {
    if (rememberReducedMotion()) return remember { mutableFloatStateOf(SHIMMER_STATIC_ALPHA) }
    return rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = SHIMMER_MIN_ALPHA,
        targetValue = SHIMMER_MAX_ALPHA,
        animationSpec = infiniteRepeatable(tween(SHIMMER_CYCLE_MS), RepeatMode.Reverse),
        label = "shimmer alpha",
    )
}
