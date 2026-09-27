package com.mk.skycast.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [Index(value = ["externalId"], unique = true)],
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Geocoding id; null for the device location. */
    val externalId: Long?,
    val name: String,
    val region: String?,
    val country: String?,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    val isDeviceLocation: Boolean,
    val sortOrder: Int,
    /** Language of name/region/country; null for rows created before v2. */
    @ColumnInfo(defaultValue = "NULL")
    val nameLanguage: String? = null,
)
