package com.mk.skycast.feature.settings.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mk.skycast.core.designsystem.components.SkyChoiceRow
import com.mk.skycast.core.designsystem.components.SkyGroup

@Composable
internal fun SettingsGroup(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SkyGroup(stringResource(title), modifier, content)
}

/** Enum choice whose labels come from string resources. */
@Composable
internal fun <T> ChoiceRow(
    @StringRes title: Int?,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SkyChoiceRow(
        title = title?.let { stringResource(it) },
        options = options,
        selected = selected,
        label = { stringResource(label(it)) },
        onSelect = onSelect,
        modifier = modifier,
    )
}
