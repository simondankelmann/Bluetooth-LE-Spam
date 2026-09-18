package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseDataManufacturerSpecificDataEntity

@Dao
interface AdvertiseDataManufacturerSpecificDataDao {
    @Query("SELECT * FROM advertisedatamanufacturerspecificdataentity WHERE id = :id")
    fun findById(id: Int): AdvertiseDataManufacturerSpecificDataEntity

    @Query("SELECT * FROM advertisedatamanufacturerspecificdataentity WHERE advertiseDataId = :id")
    fun findByAdvertiseDataId(id: Int): List<AdvertiseDataManufacturerSpecificDataEntity>

    @Query("SELECT * FROM advertisedatamanufacturerspecificdataentity")
    fun getAll(): List<AdvertiseDataManufacturerSpecificDataEntity>

    @Insert
    fun insertAll(vararg advertiseDataManufacturerSpecificDataEntity: AdvertiseDataManufacturerSpecificDataEntity)

    @Delete
    fun delete(advertiseDataManufacturerSpecificDataEntity: AdvertiseDataManufacturerSpecificDataEntity)

    @Insert
    fun insertItem(advertiseDataManufacturerSpecificDataEntity: AdvertiseDataManufacturerSpecificDataEntity): Long

    @Query("UPDATE advertisedatamanufacturerspecificdataentity SET manufacturerId = :manufacturerId, manufacturerSpecificData = :manufacturerSpecificData WHERE id = :id")
    fun updateEntry(id: Int, manufacturerId: Int, manufacturerSpecificData: String)

    @Query("DELETE FROM advertisedatamanufacturerspecificdataentity WHERE advertiseDataId = :advertiseDataId")
    fun deleteByAdvertiseDataId(advertiseDataId: Int)
}