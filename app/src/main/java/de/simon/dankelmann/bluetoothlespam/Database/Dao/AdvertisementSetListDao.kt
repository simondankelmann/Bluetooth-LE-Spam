package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity

@Dao
interface AdvertisementSetListDao {
    @Query("SELECT * FROM advertisementsetlistentity WHERE id = :id")
    fun findById(id: Int): AdvertisementSetListEntity

    @Query("SELECT * FROM advertisementsetlistentity")
    fun getAll(): List<AdvertisementSetListEntity>

    @Insert
    fun insertAll(vararg advertisementSetListEntity: AdvertisementSetListEntity)

    @Delete
    fun delete(advertisementSetListEntity: AdvertisementSetListEntity)

    @Insert
    fun insertItem(advertisementSetListEntity: AdvertisementSetListEntity): Long
}