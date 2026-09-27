package com.mk.skycast.core.domain.repository

import com.mk.skycast.core.model.AppLanguage

/**
 * Per-app language, persisted by the platform (not in user preferences) so it is
 * applied before the first frame. Must be called from the main thread.
 */
interface AppLanguageRepository {
    fun current(): AppLanguage
    fun set(language: AppLanguage)
}
