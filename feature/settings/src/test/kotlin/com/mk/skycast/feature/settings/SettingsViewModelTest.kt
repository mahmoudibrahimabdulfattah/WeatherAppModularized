package com.mk.skycast.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mk.skycast.core.domain.ai.AiConsent
import com.mk.skycast.core.domain.ai.ObserveAiAvailabilityUseCase
import com.mk.skycast.core.domain.ai.SetAiConsentUseCase
import com.mk.skycast.core.domain.brief.ResetComfortCalibrationUseCase
import com.mk.skycast.core.domain.usecase.GetAppLanguageUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.domain.usecase.PreferenceUpdate
import com.mk.skycast.core.domain.usecase.UpdateUserPreferenceUseCase
import com.mk.skycast.core.model.AppLanguage
import com.mk.skycast.core.model.TemperatureUnit
import com.mk.skycast.core.model.ThemeMode
import com.mk.skycast.core.testing.FakeAiSettingsRepository
import com.mk.skycast.core.testing.FakeAppLanguageRepository
import com.mk.skycast.core.testing.FakeComfortRepository
import com.mk.skycast.core.testing.FakeUserPreferencesRepository
import com.mk.skycast.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferencesRepository()
    private val language = FakeAppLanguageRepository()
    private val aiSettings = FakeAiSettingsRepository(AiConsent.GRANTED)
    private val vm by lazy {
        SettingsViewModel(
            ObserveUserPreferencesUseCase(preferences),
            GetAppLanguageUseCase(language),
            UpdateUserPreferenceUseCase(preferences, language),
            ObserveAiAvailabilityUseCase(aiSettings) { true },
            SetAiConsentUseCase(aiSettings),
            ResetComfortCalibrationUseCase(FakeComfortRepository()),
        )
    }

    @Test
    fun `updates are persisted and reflected in state`() = runTest {
        vm.onIntent(SettingsIntent.Update(PreferenceUpdate.Temperature(TemperatureUnit.FAHRENHEIT)))
        vm.onIntent(SettingsIntent.Update(PreferenceUpdate.Theme(ThemeMode.DARK)))

        assertThat(preferences.current.temperatureUnit).isEqualTo(TemperatureUnit.FAHRENHEIT)
        assertThat(vm.state.value.preferences.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `language change is applied and reflected in state`() = runTest {
        vm.onIntent(SettingsIntent.Update(PreferenceUpdate.Language(AppLanguage.ARABIC)))

        assertThat(language.language).isEqualTo(AppLanguage.ARABIC)
        assertThat(vm.state.value.language).isEqualTo(AppLanguage.ARABIC)
    }

    @Test
    fun `data source opens the attribution url`() = runTest {
        vm.effects.test {
            vm.onIntent(SettingsIntent.OpenDataSourceClicked)
            assertThat(awaitItem()).isEqualTo(SettingsEffect.OpenUrl(SettingsViewModel.DATA_SOURCE_URL))
        }
    }

    @Test
    fun `turning AI wording off declines, turning it on asks again`() = runTest {
        assertThat(vm.state.value.aiEnabled).isTrue()

        vm.onIntent(SettingsIntent.AiWordingToggled(false))
        assertThat(vm.state.value.aiEnabled).isFalse()

        vm.onIntent(SettingsIntent.AiWordingToggled(true))
        aiSettings.consent.test { assertThat(awaitItem()).isEqualTo(AiConsent.UNKNOWN) }
    }
}
