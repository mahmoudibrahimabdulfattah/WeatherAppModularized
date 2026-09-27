package com.mk.skycast.feature.routine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mk.skycast.core.designsystem.components.SkyChoiceRow
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.ui.format.rememberWeatherFormatter
import com.mk.skycast.feature.routine.R
import com.mk.skycast.feature.routine.RoutineIntent
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun OutingEditorSheet(
    outing: Outing,
    isNew: Boolean,
    canSave: Boolean,
    timeFormat: TimeFormat,
    onIntent: (RoutineIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = rememberWeatherFormatter(UserPreferences(timeFormat = timeFormat))
    val update: (Outing) -> Unit = { onIntent(RoutineIntent.OutingDraftChanged(it)) }
    ModalBottomSheet(
        onDismissRequest = { onIntent(RoutineIntent.DismissOutingEditor) },
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
                stringResource(if (isNew) R.string.routine_add_outing else R.string.routine_edit_outing),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
                OutingKind.entries.forEach { kind ->
                    FilterChip(
                        selected = outing.kind == kind,
                        onClick = { update(outing.copy(kind = kind)) },
                        label = { Text(stringResource(kind.labelRes())) },
                    )
                }
            }
            if (outing.kind == OutingKind.CUSTOM) {
                OutlinedTextField(
                    value = outing.customLabel.orEmpty(),
                    onValueChange = { update(outing.copy(customLabel = it)) },
                    label = { Text(stringResource(R.string.routine_custom_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(stringResource(R.string.routine_outing_days), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
                weekOrder.forEach { day ->
                    val selected = day in outing.days
                    FilterChip(
                        selected = selected,
                        onClick = {
                            update(outing.copy(days = if (selected) outing.days - day else outing.days + day))
                        },
                        label = { Text(day.displayName(TextStyle.SHORT)) },
                    )
                }
            }
            TimeRow(
                title = stringResource(R.string.routine_outing_depart),
                time = outing.departAt,
                formatter = formatter,
                onTimeChange = { update(outing.copy(departAt = it)) },
            )
            TimeRow(
                title = stringResource(R.string.routine_outing_return),
                time = outing.returnAt,
                formatter = formatter,
                onTimeChange = { update(outing.copy(returnAt = it)) },
            )
            TravelModeChips(selected = outing.mode, onSelect = { update(outing.copy(mode = it)) })
            SkyChoiceRow(
                title = stringResource(R.string.routine_outing_setting),
                options = OutingSetting.entries,
                selected = outing.setting,
                label = { stringResource(it.labelRes()) },
                onSelect = { update(outing.copy(setting = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
                if (!isNew) {
                    TextButton(
                        onClick = { onIntent(RoutineIntent.DeleteOutingClicked(outing.id)) },
                        modifier = Modifier.heightIn(min = SkySpace.touch),
                    ) {
                        Text(stringResource(R.string.routine_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = { onIntent(RoutineIntent.SaveOutingClicked) },
                    enabled = canSave,
                    modifier = Modifier.weight(1f).heightIn(min = SkySpace.touch),
                ) {
                    Text(stringResource(R.string.routine_save_outing))
                }
            }
        }
    }
}
