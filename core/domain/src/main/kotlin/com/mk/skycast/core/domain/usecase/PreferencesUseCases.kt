package com.mk.skycast.core.domain.usecase

import com.mk.skycast.core.domain.repository.AppLanguageRepository
import com.mk.skycast.core.domain.repository.UserPreferencesRepository
import com.mk.skycast.core.model.AppLanguage
import com.mk.skycast.core.model.PrecipitationUnit
import com.mk.skycast.core.model.PressureUnit
import com.mk.skycast.core.model.TemperatureUnit
import com.mk.skycast.core.model.ThemeMode
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.model.WindSpeedUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveUserPreferencesUseCase @Inject constructor(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<UserPreferences> = repository.preferences.distinctUntilChanged()
}

/** A single user-driven preference change. */
sealed interface PreferenceUpdate {
    data class Temperature(val unit: TemperatureUnit) : PreferenceUpdate
    data class WindSpeed(val unit: WindSpeedUnit) : PreferenceUpdate
    data class Precipitation(val unit: PrecipitationUnit) : PreferenceUpdate
    data class Pressure(val unit: PressureUnit) : PreferenceUpdate
    data class Clock(val format: TimeFormat) : PreferenceUpdate
    data class Theme(val mode: ThemeMode) : PreferenceUpdate
    data class DynamicColor(val enabled: Boolean) : PreferenceUpdate
    data class Language(val language: AppLanguage) : PreferenceUpdate
}

class UpdateUserPreferenceUseCase @Inject constructor(
    private val repository: UserPreferencesRepository,
    private val languageRepository: AppLanguageRepository,
) {
    suspend operator fun invoke(update: PreferenceUpdate) = when (update) {
        is PreferenceUpdate.Temperature -> repository.setTemperatureUnit(update.unit)
        is PreferenceUpdate.WindSpeed -> repository.setWindSpeedUnit(update.unit)
        is PreferenceUpdate.Precipitation -> repository.setPrecipitationUnit(update.unit)
        is PreferenceUpdate.Pressure -> repository.setPressureUnit(update.unit)
        is PreferenceUpdate.Clock -> repository.setTimeFormat(update.format)
        is PreferenceUpdate.Theme -> repository.setThemeMode(update.mode)
        is PreferenceUpdate.DynamicColor -> repository.setUseDynamicColor(update.enabled)
        is PreferenceUpdate.Language -> languageRepository.set(update.language)
    }
}

class GetAppLanguageUseCase @Inject constructor(private val languageRepository: AppLanguageRepository) {
    operator fun invoke(): AppLanguage = languageRepository.current()
}
