package com.mk.skycast.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.theme.SkySpace

/** Minimum width per segment before the row starts scrolling horizontally. */
private const val SEGMENT_MIN_WIDTH_DP = 76

/** Titled group of controls on a solid surface (settings-style screens). */
@Composable
fun SkyGroup(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.small)) {
        SkyHeading(title, Modifier.padding(start = SkySpace.small))
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(SkySpace.medium),
                verticalArrangement = Arrangement.spacedBy(SkySpace.large),
                content = content,
            )
        }
    }
}

/**
 * Single-choice segmented control. [label] maps each option to its text, so
 * adding an enum value can never silently shift labels. A null [selected] shows no selection.
 */
@Composable
fun <T> SkyChoiceRow(
    title: String?,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.small)) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Large fonts: keep segments readable and let the row scroll instead of truncating.
            val rowWidth = maxOf(maxWidth, (options.size * SEGMENT_MIN_WIDTH_DP * fontScale).dp)
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                SingleChoiceSegmentedButtonRow(Modifier.width(rowWidth)) {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == selected,
                            onClick = { onSelect(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                            modifier = Modifier.heightIn(min = SkySpace.touch),
                            icon = {},
                        ) {
                            Text(label(option), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkySwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

/** Tappable "label …… value" row, e.g. a time that opens a picker. */
@Composable
fun SkyValueRow(title: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SkySpace.touch)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
