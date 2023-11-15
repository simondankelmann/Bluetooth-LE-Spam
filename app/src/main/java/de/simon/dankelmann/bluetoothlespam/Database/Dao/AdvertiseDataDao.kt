package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseSettingsEntity

@Dao
interface AdvertiseDataDao {
    @Query("SELECT * FROM advertisedataentity WHERE id = :id")
    fun findById(id: Int): AdvertiseDataEntity

    @Query("SELECT * FROM advertisedataentity")
    fun getAll(): List<AdvertiseDataEntity>

    @Query("SELECT * FROM advertisedataentity WHERE id IN (:advertisementSetEntityIds)")
    fun loadAllByIds(advertisementSetEntityIds: IntArray): List<AdvertiseDataEntity>

    @Insert
    fun insertAll(vararg advertisementSetEntity: AdvertiseDataEntity)

    @Delete
    fun delete(advertisementSetEntity: AdvertiseDataEntity)

    @Insert
    fun insertItem(advertiseDataEntity: AdvertiseDataEntity): Long
}