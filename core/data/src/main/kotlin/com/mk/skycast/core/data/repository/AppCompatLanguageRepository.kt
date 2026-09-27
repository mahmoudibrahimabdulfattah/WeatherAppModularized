package com.mk.skycast.core.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mk.skycast.core.domain.repository.AppLanguageRepository
import com.mk.skycast.core.model.AppLanguage
import javax.inject.Inject

/**
 * Backed by the per-app language API: native `LocaleManager` on Android 13+, and
 * AppCompat's persisted backport on older versions. Also shows up in the system
 * "App languages" settings, and stays in sync with it.
 */
internal class AppCompatLanguageRepository @Inject constructor() : AppLanguageRepository {

    override fun current(): AppLanguage = AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales()[0]?.language)

    override fun set(language: AppLanguage) {
        val locales = language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
