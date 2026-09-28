package com.mk.skycast.core.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.mk.skycast.core.designsystem.components.SkyValueRow
import com.mk.skycast.core.ui.R
import com.mk.skycast.core.ui.format.WeatherFormatter
import java.time.LocalTime

/** A row showing a time; tapping it opens a Material time picker. */
@Composable
fun TimeRow(
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
        text = {
            // The app's displayLarge is the 120sp hero temperature; the picker's hour/minute
            // boxes are sized for Material's 57sp and would clip it.
            val typography = MaterialTheme.typography
            MaterialTheme(
                typography = typography.copy(
                    displayLarge = typography.displayLarge.copy(
                        fontSize = 57.sp,
                        lineHeight = 64.sp,
                        letterSpacing = 0.sp,
                    ),
                ),
            ) {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text(stringResource(R.string.core_ui_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.core_ui_cancel)) }
        },
    )
}
