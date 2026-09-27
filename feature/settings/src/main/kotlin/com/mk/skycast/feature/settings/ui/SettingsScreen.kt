package com.mk.skycast.feature.settings.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mk.skycast.core.designsystem.components.SkySwitchRow
import com.mk.skycast.core.designsystem.theme.SkySpace
import com.mk.skycast.core.domain.usecase.PreferenceUpdate
import com.mk.skycast.core.model.AppLanguage
import com.mk.skycast.core.model.PrecipitationUnit
import com.mk.skycast.core.model.PressureUnit
import com.mk.skycast.core.model.TemperatureUnit
import com.mk.skycast.core.model.ThemeMode
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.WindSpeedUnit
import com.mk.skycast.feature.settings.R
import com.mk.skycast.feature.settings.SettingsIntent
import com.mk.skycast.feature.settings.SettingsState

/** Stateless settings screen. Every change is applied instantly across the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SettingsState, onIntent: (SettingsIntent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(SettingsIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.settings_loading), Modifier.padding(SkySpace.large))
            }
        } else {
            SettingsContent(
                state = state,
                onUpdate = { onIntent(SettingsIntent.Update(it)) },
                onOpenDataSource = { onIntent(SettingsIntent.OpenDataSourceClicked) },
                onOpenRoutine = { onIntent(SettingsIntent.OpenRoutineClicked) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onUpdate: (PreferenceUpdate) -> Unit,
    onOpenDataSource: () -> Unit,
    onOpenRoutine: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = state.preferences
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = SkySpace.contentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = SkySpace.large),
            verticalArrangement = Arrangement.spacedBy(SkySpace.large),
        ) {
            Text(stringResource(R.string.settings_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)

            SettingsGroup(R.string.settings_routine) {
                RoutineEntry(onOpenRoutine)
            }

            SettingsGroup(R.string.settings_units) {
                ChoiceRow(
                    title = R.string.settings_temperature,
                    options = TemperatureUnit.entries,
                    selected = preferences.temperatureUnit,
                    label = TemperatureUnit::labelRes,
                    onSelect = { onUpdate(PreferenceUpdate.Temperature(it)) },
                )
                ChoiceRow(
                    title = R.string.settings_wind,
                    options = WindSpeedUnit.entries,
                    selected = preferences.windSpeedUnit,
                    label = WindSpeedUnit::labelRes,
                    onSelect = { onUpdate(PreferenceUpdate.WindSpeed(it)) },
                )
                ChoiceRow(
                    title = R.string.settings_precipitation,
                    options = PrecipitationUnit.entries,
                    selected = preferences.precipitationUnit,
                    label = PrecipitationUnit::labelRes,
                    onSelect = { onUpdate(PreferenceUpdate.Precipitation(it)) },
                )
                ChoiceRow(
                    title = R.string.settings_pressure,
                    options = PressureUnit.entries,
                    selected = preferences.pressureUnit,
                    label = PressureUnit::labelRes,
                    onSelect = { onUpdate(PreferenceUpdate.Pressure(it)) },
                )
            }

            SettingsGroup(R.string.settings_time) {
                ChoiceRow(
                    title = null,
                    options = TimeFormat.entries,
                    selected = preferences.timeFormat,
                    label = TimeFormat::labelRes,
                    onSelect = { onUpdate(PreferenceUpdate.Clock(it)) },
                )
            }

            SettingsGroup(R.string.settings_appearance) {
                ChoiceRow(
                    title = R.string.settings_language,
                    options = AppLanguage.entries,
                    selected = state.language,
                    label = AppLanguage::labelRes,
                    onSelect = { onUpdate(PreferenceUpdate.Language(it)) },
                )
                ChoiceRow(
                    title = R.string.settings_theme,
                    options = ThemeMode.entries,
                    selected = preferences.themeMode,
                    label = ThemeMode::labelRes,
                    onSelect = { onUpdate(PreferenceUpdate.Theme(it)) },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SkySwitchRow(
                        title = stringResource(R.string.settings_dynamic),
                        description = stringResource(R.string.settings_dynamic_hint),
                        checked = preferences.useDynamicColor,
                        onCheckedChange = { onUpdate(PreferenceUpdate.DynamicColor(it)) },
                    )
                }
            }

            SettingsGroup(R.string.settings_about) {
                AboutSection(onOpenDataSource)
            }
            Spacer(Modifier.height(SkySpace.medium))
        }
    }
}

@Composable
private fun RoutineEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SkySpace.touch)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkySpace.medium),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_routine_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_routine_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun AboutSection(onOpenDataSource: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    val versionLabel = version ?: stringResource(R.string.settings_version_unknown)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkySpace.small)) {
        Text(
            text = stringResource(R.string.settings_version, versionLabel),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpenDataSource, contentPadding = PaddingValues(vertical = SkySpace.small)) {
            Text(stringResource(R.string.settings_source), Modifier.weight(1f))
            Spacer(Modifier.width(SkySpace.small))
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Text(
            text = stringResource(R.string.settings_promise),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
