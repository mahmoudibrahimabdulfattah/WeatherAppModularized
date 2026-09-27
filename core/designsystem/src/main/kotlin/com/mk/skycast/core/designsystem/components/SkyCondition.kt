package com.mk.skycast.core.designsystem.components

/**
 * Rendering vocabulary for sky artwork. Kept independent of domain models so the
 * design system never depends on business code; `core:ui` maps domain → [SkyCondition].
 */
enum class SkyCondition { Clear, Partial, Cloud, Rain, Snow, Thunder, Fog, Unknown }

internal val SkyCondition.hasSun: Boolean get() = this == SkyCondition.Clear || this == SkyCondition.Partial

internal val SkyCondition.hasPrecipitation: Boolean
    get() = this == SkyCondition.Rain || this == SkyCondition.Thunder || this == SkyCondition.Snow
