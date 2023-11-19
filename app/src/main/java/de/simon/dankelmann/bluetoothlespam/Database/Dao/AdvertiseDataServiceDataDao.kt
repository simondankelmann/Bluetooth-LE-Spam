package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataManufacturerSpecificDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataServiceDataEntity

@Dao
interface AdvertiseDataServiceDataDao {
    @Query("SELECT * FROM advertisedataservicedataentity WHERE id = :id")
    fun findById(id: Int): AdvertiseDataServiceDataEntity

    @Query("SELECT * FROM advertisedataservicedataentity WHERE advertiseDataId = :id")
    fun findByAdvertiseDataId(id: Int): List<AdvertiseDataServiceDataEntity>

    @Query("SELECT * FROM advertisedataservicedataentity")
    fun getAll(): List<AdvertiseDataServiceDataEntity>

    @Insert
    fun insertAll(vararg advertiseDataServiceDataEntity: AdvertiseDataServiceDataEntity)

    @Delete
    fun delete(advertiseDataServiceDataEntity: AdvertiseDataServiceDataEntity)

    @Insert
    fun insertItem(advertiseDataServiceDataEntity: AdvertiseDataServiceDataEntity): Long
}