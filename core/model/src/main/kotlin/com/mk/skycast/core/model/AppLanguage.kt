package com.mk.skycast.core.model

/** In-app language choice. [SYSTEM] follows the device language. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ARABIC("ar"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag != null && it.tag == tag } ?: SYSTEM
    }
}
