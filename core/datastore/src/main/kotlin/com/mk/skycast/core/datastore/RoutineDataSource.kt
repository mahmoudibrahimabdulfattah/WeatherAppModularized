package com.mk.skycast.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mk.skycast.core.model.Commute
import com.mk.skycast.core.model.DayPlanOverride
import com.mk.skycast.core.model.DayType
import com.mk.skycast.core.model.Outing
import com.mk.skycast.core.model.OutingKind
import com.mk.skycast.core.model.OutingSetting
import com.mk.skycast.core.model.Routine
import com.mk.skycast.core.model.TravelMode
import java.io.IOException
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class RoutineDataSource @Inject constructor(private val dataStore: DataStore<Preferences>, private val clock: Clock) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val routine: Flow<Routine> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.ROUTINE].decodeRoutine() }

    val overrides: Flow<List<DayPlanOverride>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[Keys.OVERRIDES].decodeOverrides() }

    suspend fun save(routine: Routine) {
        dataStore.edit { it[Keys.ROUTINE] = json.encodeToString(RoutineDto.fromDomain(routine)) }
    }

    suspend fun setOverride(override: DayPlanOverride) {
        dataStore.edit { prefs ->
            val today = LocalDate.now(clock)
            val updated = (prefs[Keys.OVERRIDES].decodeOverrides() + override)
                .filterNot { it.date.isBefore(today) }
                .distinctByDateReplacingWithLast()
            prefs[Keys.OVERRIDES] = json.encodeToString(
                ListSerializer(DayPlanOverrideDto.serializer()),
                updated.map(DayPlanOverrideDto::fromDomain),
            )
        }
    }

    suspend fun clearOverride(date: LocalDate) {
        dataStore.edit { prefs ->
            val today = LocalDate.now(clock)
            val updated = prefs[Keys.OVERRIDES].decodeOverrides()
                .filterNot { it.date == date }
                .filterNot { it.date.isBefore(today) }
            prefs[Keys.OVERRIDES] = json.encodeToString(
                ListSerializer(DayPlanOverrideDto.serializer()),
                updated.map(DayPlanOverrideDto::fromDomain),
            )
        }
    }

    private fun String?.decodeRoutine(): Routine = runCatching {
        this?.let { json.decodeFromString<RoutineDto>(it).toDomain() }
    }.getOrNull() ?: Routine()

    private fun String?.decodeOverrides(): List<DayPlanOverride> = runCatching {
        this?.let {
            json.decodeFromString(ListSerializer(DayPlanOverrideDto.serializer()), it).map(DayPlanOverrideDto::toDomain)
        }
    }.getOrNull().orEmpty()

    private object Keys {
        val ROUTINE = stringPreferencesKey("routine_json")
        val OVERRIDES = stringPreferencesKey("routine_overrides_json")
    }
}

private fun List<DayPlanOverride>.distinctByDateReplacingWithLast(): List<DayPlanOverride> =
    associateBy { it.date }.values.sortedBy { it.date }

@Serializable
private data class RoutineDto(
    val isConfigured: Boolean = false,
    val week: Map<String, String> = emptyMap(),
    val commute: CommuteDto = CommuteDto(),
    val outings: List<OutingDto> = emptyList(),
    val briefTime: String = "21:00",
    val morningRefresh: Boolean = false,
    val locationId: Long? = null,
) {
    fun toDomain(): Routine {
        val defaults = Routine()
        return Routine(
            isConfigured = isConfigured,
            week = defaults.week + week.mapNotNull { (day, type) ->
                val key = day.toDayOfWeekOrNull() ?: return@mapNotNull null
                val value = type.toEnumOrNull<DayType>() ?: return@mapNotNull null
                key to value
            },
            commute = commute.toDomain(),
            outings = outings.mapNotNull(OutingDto::toDomainOrNull),
            briefTime = briefTime.toLocalTimeOrNull() ?: defaults.briefTime,
            morningRefresh = morningRefresh,
            locationId = locationId,
        )
    }

    companion object {
        fun fromDomain(routine: Routine) = RoutineDto(
            isConfigured = routine.isConfigured,
            week = routine.week.mapKeys { it.key.name }.mapValues { it.value.name },
            commute = CommuteDto.fromDomain(routine.commute),
            outings = routine.outings.map(OutingDto::fromDomain),
            briefTime = routine.briefTime.toString(),
            morningRefresh = routine.morningRefresh,
            locationId = routine.locationId,
        )
    }
}

@Serializable
private data class CommuteDto(
    val leaveHome: String = "08:00",
    val leaveWork: String = "17:00",
    val returnsNextDay: Boolean = false,
    val travelMinutes: Int = 30,
    val mode: String? = null,
) {
    fun toDomain(): Commute {
        val defaults = Commute()
        return Commute(
            leaveHome = leaveHome.toLocalTimeOrNull() ?: defaults.leaveHome,
            leaveWork = leaveWork.toLocalTimeOrNull() ?: defaults.leaveWork,
            returnsNextDay = returnsNextDay,
            travelMinutes = travelMinutes.takeIf { it > 0 } ?: defaults.travelMinutes,
            mode = mode.toEnumOrNull<TravelMode>(),
        )
    }

    companion object {
        fun fromDomain(commute: Commute) = CommuteDto(
            leaveHome = commute.leaveHome.toString(),
            leaveWork = commute.leaveWork.toString(),
            returnsNextDay = commute.returnsNextDay,
            travelMinutes = commute.travelMinutes,
            mode = commute.mode?.name,
        )
    }
}

@Serializable
private data class OutingDto(
    val id: String,
    val kind: String,
    val customLabel: String? = null,
    val days: List<String> = emptyList(),
    val departAt: String,
    val returnAt: String,
    val mode: String,
    val setting: String,
) {
    fun toDomainOrNull(): Outing? {
        val kind = kind.toEnumOrNull<OutingKind>() ?: return null
        val departAt = departAt.toLocalTimeOrNull() ?: return null
        val returnAt = returnAt.toLocalTimeOrNull() ?: return null
        return Outing(
            id = id,
            kind = kind,
            customLabel = customLabel,
            days = days.mapNotNull(String::toDayOfWeekOrNull).toSet(),
            departAt = departAt,
            returnAt = returnAt,
            mode = mode.toEnumOrNull<TravelMode>() ?: return null,
            setting = setting.toEnumOrNull<OutingSetting>() ?: return null,
        )
    }

    companion object {
        fun fromDomain(outing: Outing) = OutingDto(
            id = outing.id,
            kind = outing.kind.name,
            customLabel = outing.customLabel,
            days = outing.days.map { it.name },
            departAt = outing.departAt.toString(),
            returnAt = outing.returnAt.toString(),
            mode = outing.mode.name,
            setting = outing.setting.name,
        )
    }
}

@Serializable
private data class DayPlanOverrideDto(
    val date: String,
    val dayType: String? = null,
    val addedOutings: List<OutingDto> = emptyList(),
    val cancelledOutingIds: Set<String> = emptySet(),
) {
    fun toDomain(): DayPlanOverride = DayPlanOverride(
        date = date.toLocalDateOrNull() ?: LocalDate.MIN,
        dayType = dayType.toEnumOrNull<DayType>(),
        addedOutings = addedOutings.mapNotNull(OutingDto::toDomainOrNull),
        cancelledOutingIds = cancelledOutingIds,
    )

    companion object {
        fun fromDomain(override: DayPlanOverride) = DayPlanOverrideDto(
            date = override.date.toString(),
            dayType = override.dayType?.name,
            addedOutings = override.addedOutings.map(OutingDto::fromDomain),
            cancelledOutingIds = override.cancelledOutingIds,
        )
    }
}

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }

private fun String.toDayOfWeekOrNull(): DayOfWeek? = toEnumOrNull<DayOfWeek>()

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()
