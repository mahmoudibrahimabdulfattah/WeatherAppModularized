package com.mk.skycast.feature.routine.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mk.skycast.core.designsystem.components.SkyValueRow
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.feature.routine.R
import java.time.LocalTime

/** A row showing a time; tapping it opens a Material time picker. */
@Composable
internal fun TimeRow(
    title: String,
    time: LocalTime,
    formatter: WeatherFormatter,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    SkyValueRow(title = title, value = formatter.time(time), onClick = { picking = true }, modifier = modifier)
    if (picking) {
        TimePickerDialog(
            title = title,
            initial = time,
            is24Hour = formatter.uses24HourClock(),
            onConfirm = {
                picking = false
                onTimeChange(it)
            },
            onDismiss = { picking = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initial: LocalTime,
    is24Hour: Boolean,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberTimePickerState(initial.hour, initial.minute, is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.routine_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.routine_cancel)) }
        },
    )
}
