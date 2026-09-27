package com.mk.skycast.feature.settings.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mk.skycast.core.designsystem.theme.SkycastTheme
import com.mk.skycast.feature.settings.SettingsState

private val previewState = SettingsState(isLoading = false)

@Preview(name = "Settings · light", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsLightPreview() = SkycastTheme { SettingsScreen(previewState, onIntent = {}) }

@Preview(name = "Settings · dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 393, heightDp = 852)
@Composable
private fun SettingsDarkPreview() = SkycastTheme(darkTheme = true) { SettingsScreen(previewState, onIntent = {}) }

@Preview(name = "Settings · Arabic", locale = "ar", widthDp = 393, heightDp = 852)
@Composable
private fun SettingsArabicPreview() = SkycastTheme { SettingsScreen(previewState, onIntent = {}) }

@Preview(name = "Settings · large text", widthDp = 393, heightDp = 852, fontScale = 1.6f)
@Composable
private fun SettingsLargeTextPreview() = SkycastTheme { SettingsScreen(previewState, onIntent = {}) }
