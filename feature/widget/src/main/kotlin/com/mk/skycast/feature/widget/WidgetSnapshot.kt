package com.mk.skycast.feature.widget

import android.content.Context
import com.mk.skycast.core.domain.brief.ObserveDailyBriefUseCase
import com.mk.skycast.core.domain.usecase.ObserveLocationWeatherUseCase
import com.mk.skycast.core.domain.usecase.ObserveRoutineUseCase
import com.mk.skycast.core.domain.usecase.ObserveUserPreferencesUseCase
import com.mk.skycast.core.model.LocationWeather
import com.mk.skycast.core.ui.brief.BriefText
import com.mk.skycast.core.ui.brief.labelRes
import com.mk.skycast.core.ui.format.WeatherFormatter
import com.mk.skycast.core.ui.text.withAppLocale
import com.mk.skycast.core.ui.weather.labelRes
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Everything the widget shows, already worded in the app's language. */
data class WidgetSnapshot(
    val locationName: String?,
    val temperature: String?,
    val condition: String?,
    /** The brief's one decision, or a prompt to set up the routine. */
    val headline: String,
    /** Why / what to wear, shown when the widget is tall enough. */
    val detail: String?,
    val carry: String?,
)

class WidgetSnapshotLoader @Inject constructor(
    private val observeLocationWeather: ObserveLocationWeatherUseCase,
    private val observePreferences: ObserveUserPreferencesUseCase,
    private val observeRoutine: ObserveRoutineUseCase,
    private val observeDailyBrief: ObserveDailyBriefUseCase,
    private val clock: Clock,
) {
    suspend fun load(context: Context): WidgetSnapshot {
        val localized = context.withAppLocale()
        val resources = localized.resources
        val preferences = observePreferences().first()
        val locations = observeLocationWeather().first()
        val routine = observeRoutine().first()
        val brief = if (routine.isConfigured) observeDailyBrief().first() else null
        val shown = locations.pick(brief?.locationId ?: preferences.selectedLocationId)
            ?: return WidgetSnapshot(null, null, null, resources.getString(R.string.widget_no_location), null, null)
        val weather = shown.weather
        val formatter = WeatherFormatter(
            preferences,
            resources.configuration.locales[0],
            android.text.format.DateFormat.is24HourFormat(localized),
        )
        val text = weather?.let { BriefText(resources, formatter, it.zoneId) }
        return WidgetSnapshot(
            locationName = shown.location.name,
            temperature = weather?.let { formatter.temperature(it.current.temperatureC) },
            condition = weather?.let { resources.getString(it.current.condition.labelRes()) },
            headline = if (brief != null && text != null) {
                text.headline(brief, clock.instant())
            } else {
                resources.getString(R.string.widget_setup)
            },
            detail = brief?.let { text?.wear(it) ?: text?.dayOutlook(it) },
            carry = brief?.carry?.takeIf { it.isNotEmpty() }
                ?.joinToString(" · ") { resources.getString(it.item.labelRes()) },
        )
    }

    private fun List<LocationWeather>.pick(id: Long?): LocationWeather? =
        firstOrNull { it.location.id == id } ?: firstOrNull { it.location.isDeviceLocation } ?: firstOrNull()
}
