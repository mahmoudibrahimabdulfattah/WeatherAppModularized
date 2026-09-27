package com.mk.skycast.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mk.skycast.core.designsystem.components.SkyChoiceRow
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.ui.brief.BriefText
import com.mk.skycast.core.ui.component.TimeRow
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.feature.home.HomeIntent
import com.mk.skycast.feature.home.PlanEditor
import com.mk.skycast.feature.home.R

/** "Plans changed?" — adjusts one day only, the routine stays as it is. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanSheet(
    editor: PlanEditor,
    formatter: WeatherFormatter,
    briefText: BriefText,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(HomeIntent.PlanDismissed) },
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SkySpace.large)
                .padding(bottom = SkySpace.large),
            verticalArrangement = Arrangement.spacedBy(SkySpace.large),
        ) {
            Text(
                stringResource(R.string.home_plan_title, formatter.dayOfWeekFull(editor.date)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.home_plan_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            SkyChoiceRow(
                title = null,
                options = DayType.entries,
                selected = editor.dayType,
                label = { stringResource(it.planLabelRes()) },
                onSelect = { onIntent(HomeIntent.PlanDayTypeChanged(it)) },
            )
            editor.usualOutings.forEach { outing ->
                val going = outing.id !in editor.draft.cancelledOutingIds
                val title = briefText.outingLabel(outing)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(
                                R.string.home_plan_times,
                                formatter.time(outing.departAt),
                                formatter.time(outing.returnAt),
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = going,
                        onCheckedChange = { onIntent(HomeIntent.PlanOutingToggled(outing.id, it)) },
                        modifier = Modifier.semantics { contentDescription = title },
                    )
                }
            }
            editor.draft.addedOutings.forEach { outing -> AddedOuting(outing, formatter, onIntent) }
            OutlinedButton(
                onClick = { onIntent(HomeIntent.PlanAddOutingClicked) },
                modifier = Modifier.fillMaxWidth().heightIn(min = SkySpace.touch),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.home_plan_add_outing))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
                if (editor.hasSavedOverride) {
                    TextButton(
                        onClick = { onIntent(HomeIntent.PlanResetClicked) },
                        modifier = Modifier.heightIn(min = SkySpace.touch),
                    ) { Text(stringResource(R.string.home_plan_reset)) }
                }
                Button(
                    onClick = { onIntent(HomeIntent.PlanSaveClicked) },
                    modifier = Modifier.weight(1f).heightIn(min = SkySpace.touch),
                ) { Text(stringResource(R.string.home_plan_save)) }
            }
        }
    }
}

@Composable
private fun AddedOuting(
    outing: Outing,
    formatter: WeatherFormatter,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val update: (Outing) -> Unit = { onIntent(HomeIntent.PlanAddedOutingChanged(it)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.small)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_plan_one_time),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onIntent(HomeIntent.PlanAddedOutingRemoved(outing.id)) }) {
                Icon(Icons.Rounded.Close, stringResource(R.string.home_plan_remove))
            }
        }
        TimeRow(
            title = stringResource(R.string.home_plan_leave),
            time = outing.departAt,
            formatter = formatter,
            onTimeChange = { update(outing.copy(departAt = it)) },
        )
        TimeRow(
            title = stringResource(R.string.home_plan_return),
            time = outing.returnAt,
            formatter = formatter,
            onTimeChange = { update(outing.copy(returnAt = it)) },
        )
        SkyChoiceRow(
            title = null,
            options = OutingSetting.entries,
            selected = outing.setting,
            label = {
                stringResource(
                    if (it == OutingSetting.OUTDOORS) R.string.home_plan_outdoors else R.string.home_plan_indoors,
                )
            },
            onSelect = { update(outing.copy(setting = it)) },
        )
    }
}

private fun DayType.planLabelRes(): Int = when (this) {
    DayType.AWAY -> R.string.home_plan_away
    DayType.HOME -> R.string.home_plan_home
    DayType.OFF -> R.string.home_plan_off
}
