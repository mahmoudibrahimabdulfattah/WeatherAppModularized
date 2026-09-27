package com.mk.skycast.feature.routine.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.mk.skycast.core.designsystem.components.SkyChoiceRow
import com.mk.skycast.core.designsystem.components.SkyGroup
import com.mk.skycast.core.designsystem.components.SkySwitchRow
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import com.mk.skycast.core.ui.component.TimeRow
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.feature.routine.R
import com.mk.skycast.feature.routine.RoutineIntent
import com.mk.skycast.feature.routine.RoutineState
import java.time.DayOfWeek

@Composable
internal fun WeekStep(week: Map<DayOfWeek, DayType>, onIntent: (RoutineIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.large)) {
        StepIntro(R.string.routine_week_intro)
        SkyGroup(stringResource(R.string.routine_week)) {
            weekOrder.forEach { day ->
                SkyChoiceRow(
                    title = day.displayName(),
                    options = DayType.entries,
                    selected = week[day],
                    label = { stringResource(it.labelRes()) },
                    onSelect = { onIntent(RoutineIntent.DayTypeChanged(day, it)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TripsStep(
    state: RoutineState,
    formatter: WeatherFormatter,
    onIntent: (RoutineIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val commute = state.draft.commute
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.large)) {
        if (state.showsCommute) {
            StepIntro(R.string.routine_trip_intro)
            SkyGroup(stringResource(R.string.routine_trip)) {
                TimeRow(
                    title = stringResource(R.string.routine_leave_home),
                    time = commute.leaveHome,
                    formatter = formatter,
                    onTimeChange = { onIntent(RoutineIntent.LeaveHomeChanged(it)) },
                )
                TimeRow(
                    title = stringResource(R.string.routine_leave_work),
                    time = commute.leaveWork,
                    formatter = formatter,
                    onTimeChange = { onIntent(RoutineIntent.LeaveWorkChanged(it)) },
                )
                SkySwitchRow(
                    title = stringResource(R.string.routine_next_day),
                    description = stringResource(R.string.routine_next_day_hint),
                    checked = commute.returnsNextDay,
                    onCheckedChange = { onIntent(RoutineIntent.ReturnsNextDayChanged(it)) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.routine_travel_time), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
                    travelMinuteOptions.forEach { minutes ->
                        FilterChip(
                            selected = commute.travelMinutes == minutes,
                            onClick = { onIntent(RoutineIntent.TravelMinutesChanged(minutes)) },
                            label = { Text(stringResource(R.string.routine_minutes, formatter.number(minutes))) },
                        )
                    }
                }
                TravelModeChips(selected = commute.mode, onSelect = { onIntent(RoutineIntent.TravelModeChanged(it)) })
                if (state.showCommuteModeError) {
                    Text(
                        stringResource(R.string.routine_transport_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            StepIntro(R.string.routine_outings_intro_no_trip)
        }
        SkyGroup(stringResource(R.string.routine_outings)) {
            if (state.draft.outings.isEmpty()) {
                Text(
                    stringResource(R.string.routine_outings_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.draft.outings.forEach { outing ->
                OutingRow(outing, formatter, onClick = { onIntent(RoutineIntent.EditOutingClicked(outing.id)) })
            }
            OutlinedButton(
                onClick = { onIntent(RoutineIntent.AddOutingClicked) },
                modifier = Modifier.fillMaxWidth().heightIn(min = SkySpace.touch),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.routine_add_outing))
            }
        }
    }
}

@Composable
private fun OutingRow(outing: Outing, formatter: WeatherFormatter, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SkySpace.touch)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(outing.title(), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.routine_outing_summary,
                    outing.days.shortList(),
                    formatter.time(outing.departAt),
                    formatter.time(outing.returnAt),
                    stringResource(outing.setting.labelRes()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Wrapping chips: five transport options never fit one segmented row on a phone. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TravelModeChips(selected: TravelMode?, onSelect: (TravelMode) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.small)) {
        Text(stringResource(R.string.routine_transport), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SkySpace.small)) {
            TravelMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selected,
                    onClick = { onSelect(mode) },
                    label = { Text(stringResource(mode.labelRes())) },
                )
            }
        }
    }
}

@Composable
internal fun BriefStep(
    routine: Routine,
    formatter: WeatherFormatter,
    onIntent: (RoutineIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.large)) {
        StepIntro(R.string.routine_brief_intro)
        SkyGroup(stringResource(R.string.routine_brief)) {
            TimeRow(
                title = stringResource(R.string.routine_brief_time),
                time = routine.briefTime,
                formatter = formatter,
                onTimeChange = { onIntent(RoutineIntent.BriefTimeChanged(it)) },
            )
            SkySwitchRow(
                title = stringResource(R.string.routine_morning_refresh),
                description = stringResource(R.string.routine_morning_refresh_hint),
                checked = routine.morningRefresh,
                onCheckedChange = { onIntent(RoutineIntent.MorningRefreshChanged(it)) },
            )
        }
    }
}

@Composable
private fun StepIntro(text: Int, modifier: Modifier = Modifier) {
    Text(stringResource(text), modifier = modifier, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
