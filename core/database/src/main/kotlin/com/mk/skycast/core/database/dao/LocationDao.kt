package com.mk.skycast.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.mk.skycast.core.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY isDeviceLocation DESC, sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY isDeviceLocation DESC, sortOrder ASC, id ASC")
    suspend fun getAll(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getById(id: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE externalId = :externalId")
    suspend fun getByExternalId(externalId: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE isDeviceLocation = 1 LIMIT 1")
    suspend fun getDeviceLocation(): LocationEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM locations")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LocationEntity): Long

    @Upsert
    suspend fun upsert(entity: LocationEntity): Long

    @Query("SELECT * FROM locations WHERE isDeviceLocation = 0 AND (nameLanguage IS NULL OR nameLanguage != :language)")
    suspend fun getNotLocalizedTo(language: String): List<LocationEntity>

    @Query(
        "UPDATE locations SET name = :name, region = :region, country = :country, nameLanguage = :language " +
            "WHERE id = :id",
    )
    suspend fun updateNames(id: Long, name: String, region: String?, country: String?, language: String)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE locations SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> updateSortOrder(id, index) }
    }
}
