package com.mk.skycast.core.testing

import com.mk.skycast.core.common.DataError
import com.mk.skycast.core.common.LocationError
import com.mk.skycast.core.common.Outcome
import com.mk.skycast.core.common.TimeTicker
import com.mk.skycast.core.common.map
import com.mk.skycast.core.domain.repository.AppLanguageRepository
import com.mk.skycast.core.domain.repository.DeviceLocationProvider
import com.mk.skycast.core.domain.repository.LocationRepository
import com.mk.skycast.core.domain.repository.NetworkMonitor
import com.mk.skycast.core.domain.repository.PlaceSearchRepository
import com.mk.skycast.core.domain.repository.RoutineRepository
import com.mk.skycast.core.domain.repository.UserPreferencesRepository
import com.mk.skycast.core.domain.repository.WeatherRepository
import com.mk.skycast.core.domain.repository.WeatherSyncScheduler
import com.mk.skycast.core.model.AppLanguage
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.DeviceLocation
import com.mk.skycast.core.model.PlaceSuggestion
import com.mk.skycast.core.model.PrecipitationUnit
import com.mk.skycast.core.model.PressureUnit
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.SavedLocation
import com.mk.skycast.core.model.TemperatureUnit
import com.mk.skycast.core.model.ThemeMode
import com.mk.skycast.core.model.TimeFormat
import com.mk.skycast.core.model.UserPreferences
import com.mk.skycast.core.model.Weather
import com.mk.skycast.core.model.WindSpeedUnit
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeWeatherRepository : WeatherRepository {
    private val store = MutableStateFlow<Map<Long, Weather>>(emptyMap())
    val refreshCalls = mutableListOf<Pair<Long, Boolean>>()
    var nextRefreshResult: Outcome<Unit, DataError> = Outcome.Success(Unit)
    var weatherFactory: (Long) -> Weather? = { TestData.weather(it) }

    fun emit(weather: Weather) = store.update { it + (weather.locationId to weather) }

    override fun observeWeather(locationId: Long): Flow<Weather?> = store.map { it[locationId] }

    override suspend fun refresh(locationId: Long, force: Boolean): Outcome<Unit, DataError> {
        refreshCalls += locationId to force
        val result = nextRefreshResult
        if (result is Outcome.Success) weatherFactory(locationId)?.let(::emit)
        return result
    }
}

class FakeLocationRepository(initial: List<SavedLocation> = emptyList()) : LocationRepository {
    val locations = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeSavedLocations(): Flow<List<SavedLocation>> = locations.map { list ->
        list.sortedWith(compareByDescending<SavedLocation> { it.isDeviceLocation }.thenBy { it.sortOrder })
    }

    override suspend fun getSavedLocations(): List<SavedLocation> = locations.value

    override suspend fun save(place: PlaceSuggestion): Long {
        val id = nextId++
        locations.update {
            it + SavedLocation(
                id = id, name = place.name, region = place.region, country = place.country,
                countryCode = place.countryCode, point = place.point, timezone = place.timezone,
                isDeviceLocation = false, sortOrder = it.size,
            )
        }
        return id
    }

    override suspend fun saveDeviceLocation(location: DeviceLocation): Long {
        val existing = locations.value.firstOrNull { it.isDeviceLocation }
        val id = existing?.id ?: nextId++
        val entry = SavedLocation(
            id = id, name = location.name ?: existing?.name.orEmpty(), region = location.region,
            country = location.country, countryCode = location.countryCode, point = location.point,
            timezone = existing?.timezone, isDeviceLocation = true, sortOrder = -1,
            nameLanguage = location.languageCode,
        )
        locations.update { list -> list.filterNot { it.id == id } + entry }
        return id
    }

    override suspend fun remove(locationId: Long) = locations.update { list -> list.filterNot { it.id == locationId } }

    override suspend fun reorder(orderedIds: List<Long>) = locations.update { list ->
        list.map { loc -> loc.copy(sortOrder = orderedIds.indexOf(loc.id).takeIf { it >= 0 } ?: Int.MAX_VALUE) }
    }

    /** Simulates the remote lookup by suffixing names with the language code. */
    override suspend fun localizeNames(languageCode: String): Outcome<Unit, DataError> {
        locations.update { list ->
            list.map { loc ->
                if (loc.isDeviceLocation || loc.nameLanguage == languageCode) {
                    loc
                } else {
                    loc.copy(name = "${loc.name.substringBefore(" [")} [$languageCode]", nameLanguage = languageCode)
                }
            }
        }
        return Outcome.Success(Unit)
    }
}

class FakeUserPreferencesRepository(initial: UserPreferences = UserPreferences()) : UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state
    val current: UserPreferences get() = state.value

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) = state.update { it.copy(temperatureUnit = unit) }
    override suspend fun setWindSpeedUnit(unit: WindSpeedUnit) = state.update { it.copy(windSpeedUnit = unit) }
    override suspend fun setPrecipitationUnit(unit: PrecipitationUnit) =
        state.update { it.copy(precipitationUnit = unit) }
    override suspend fun setPressureUnit(unit: PressureUnit) = state.update { it.copy(pressureUnit = unit) }
    override suspend fun setTimeFormat(format: TimeFormat) = state.update { it.copy(timeFormat = format) }
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setUseDynamicColor(enabled: Boolean) = state.update { it.copy(useDynamicColor = enabled) }
    override suspend fun setSelectedLocationId(id: Long?) = state.update { it.copy(selectedLocationId = id) }
}

class FakePlaceSearchRepository : PlaceSearchRepository {
    var result: Outcome<List<PlaceSuggestion>, DataError> = Outcome.Success(listOf(TestData.place()))
    val queries = mutableListOf<String>()

    override suspend fun search(query: String, languageCode: String): Outcome<List<PlaceSuggestion>, DataError> {
        queries += query
        return result
    }
}

class FakeDeviceLocationProvider : DeviceLocationProvider {
    var permissionGranted = true
    var result: Outcome<DeviceLocation, LocationError> = Outcome.Success(
        DeviceLocation(TestData.location().point, "Cairo", null, "Egypt", "EG"),
    )

    val requestedLanguages = mutableListOf<String?>()

    override fun hasPermission(): Boolean = permissionGranted

    override suspend fun currentLocation(languageCode: String?): Outcome<DeviceLocation, LocationError> {
        requestedLanguages += languageCode
        if (!permissionGranted) return Outcome.Failure(LocationError.PermissionDenied)
        return result.map { it.copy(languageCode = languageCode ?: it.languageCode) }
    }
}

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {
    val online = MutableStateFlow(online)
    override val isOnline: Flow<Boolean> = this.online
}

class FakeWeatherSyncScheduler : WeatherSyncScheduler {
    var periodicScheduled = false
    var immediateRequests = 0
    override fun schedulePeriodicSync() {
        periodicScheduled = true
    }
    override fun requestImmediateSync() {
        immediateRequests++
    }
}

class FakeTimeTicker(now: java.time.Instant = TestData.NOW) : TimeTicker {
    val now = MutableStateFlow(now)
    override val ticks: Flow<java.time.Instant> = this.now
}

class FakeAppLanguageRepository(var language: AppLanguage = AppLanguage.SYSTEM) : AppLanguageRepository {
    override fun current(): AppLanguage = language
    override fun set(language: AppLanguage) {
        this.language = language
    }
}

class FakeRoutineRepository(initial: Routine = Routine()) : RoutineRepository {
    private val routineState = MutableStateFlow(initial)
    private val overridesState = MutableStateFlow<List<DayPlanOverride>>(emptyList())
    override val routine: Flow<Routine> = routineState
    override val overrides: Flow<List<DayPlanOverride>> = overridesState
    val current: Routine get() = routineState.value
    val currentOverrides: List<DayPlanOverride> get() = overridesState.value

    override suspend fun save(routine: Routine) = routineState.update { routine }

    override suspend fun setOverride(override: DayPlanOverride) =
        overridesState.update { list -> list.filterNot { it.date == override.date } + override }

    override suspend fun clearOverride(date: LocalDate) = overridesState.update { list ->
        list.filterNot {
            it.date ==
                date
        }
    }
}
