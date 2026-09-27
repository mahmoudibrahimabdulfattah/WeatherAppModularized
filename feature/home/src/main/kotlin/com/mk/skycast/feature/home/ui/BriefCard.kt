package com.mk.skycast.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Masks
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.mk.skycast.core.designsystem.components.GlassCard
import com.mk.skycast.core.designsystem.theme.SkyColors
import com.mk.skycast.core.designsystem.theme.SkyIconSize
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.CarryItem
import com.mk.skycast.core.model.DailyBrief
import com.mk.skycast.core.model.ForecastCoverage
import com.mk.skycast.core.ui.brief.BriefText
import com.mk.skycast.core.ui.brief.labelRes
import com.mk.skycast.feature.home.R
import java.time.Instant

/** "Tomorrow, prepared": the one decision first, details on demand. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BriefCard(
    brief: DailyBrief,
    text: BriefText,
    now: Instant,
    expanded: Boolean,
    hasPlanChange: Boolean,
    onToggleExpand: () -> Unit,
    onChangePlans: () -> Unit,
    onEditRoutine: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle(Icons.Rounded.EventAvailable, text.title(brief, now), Modifier.weight(1f))
            IconButton(onClick = onEditRoutine) {
                Icon(Icons.Rounded.Tune, stringResource(R.string.home_brief_edit_routine), tint = SkyColors.MutedSky)
            }
        }
        Text(
            text = text.routineSummary(brief) +
                if (hasPlanChange) " · " + stringResource(R.string.home_brief_changed) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = SkyColors.MutedSky,
        )
        Text(text.headline(brief, now), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        text.wear(brief)?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        if (!brief.hasOutings) Text(text.dayOutlook(brief), style = MaterialTheme.typography.bodyLarge)
        if (brief.carry.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SkySpace.small),
                verticalArrangement = Arrangement.spacedBy(SkySpace.small),
            ) {
                brief.carry.forEach { CarryPill(it.item.icon(), stringResource(it.item.labelRes())) }
            }
        }
        AnimatedVisibility(visible = expanded) { BriefDetails(brief, text) }
        Row(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
            TextButton(onClick = onChangePlans, modifier = Modifier.heightIn(min = SkySpace.touch)) {
                Text(stringResource(R.string.home_brief_plans_changed), color = SkyColors.OnSky)
            }
            TextButton(onClick = onToggleExpand, modifier = Modifier.heightIn(min = SkySpace.touch)) {
                Text(
                    stringResource(if (expanded) R.string.home_brief_less else R.string.home_brief_more),
                    color = SkyColors.OnSky,
                )
            }
        }
    }
}

@Composable
private fun BriefDetails(brief: DailyBrief, text: BriefText, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.medium)) {
        HorizontalDivider(color = SkyColors.Divider)
        brief.hazards.forEach { DetailLine(Icons.Rounded.WarningAmber, text.hazard(it)) }
        brief.carry.forEach { DetailLine(it.item.icon(), text.carry(it)) }
        brief.windows.forEach { outlook ->
            Column {
                Text(text.windowLabel(outlook.window), style = MaterialTheme.typography.titleSmall)
                Text(
                    text.windowDetail(outlook),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyColors.MutedSky,
                )
            }
        }
        if (brief.hasOutings) Text(text.dayOutlook(brief), color = SkyColors.MutedSky)
        if (brief.coverage == ForecastCoverage.PARTIAL) {
            Text(stringResource(R.string.home_brief_partial), color = SkyColors.MutedSky)
        }
        Text(
            stringResource(R.string.home_brief_based_on, text.time(brief.forecastFetchedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = SkyColors.MutedSky,
        )
    }
}

@Composable
private fun DetailLine(icon: ImageVector, line: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(SkyIconSize.small), tint = SkyColors.Sun)
        Text(line, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CarryPill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = SkyColors.Track, contentColor = SkyColors.OnSky) {
        Row(
            modifier = Modifier.padding(horizontal = SkySpace.medium, vertical = SkySpace.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SkySpace.small),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(SkyIconSize.small))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Shown on the brief's page until the routine is set up. */
@Composable
internal fun RoutinePromptCard(onSetUp: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier.fillMaxWidth()) {
        CardTitle(Icons.Rounded.EventAvailable, stringResource(R.string.home_brief_prompt_title))
        Text(stringResource(R.string.home_brief_prompt_body), style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = onSetUp,
            modifier = Modifier.heightIn(min = SkySpace.touch),
            colors = ButtonDefaults.buttonColors(containerColor = SkyColors.OnSky, contentColor = SkyColors.Ink),
        ) {
            Text(stringResource(R.string.home_brief_prompt_action))
        }
    }
}

private fun CarryItem.icon(): ImageVector = when (this) {
    CarryItem.UMBRELLA, CarryItem.RAINCOAT -> Icons.Rounded.Umbrella
    CarryItem.EXTRA_LAYER -> Icons.Rounded.Checkroom
    CarryItem.SUNSCREEN -> Icons.Rounded.WbSunny
    CarryItem.WATER -> Icons.Rounded.WaterDrop
    CarryItem.MASK -> Icons.Rounded.Masks
}
