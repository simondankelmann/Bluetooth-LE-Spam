package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget

@Dao
interface AdvertisementSetDao {

    @Query("SELECT * FROM advertisementsetentity WHERE id = :id")
    fun findById(id: Int): AdvertisementSetEntity

    @Query("SELECT * FROM advertisementsetentity")
    fun getAll(): List<AdvertisementSetEntity>

    @Query("SELECT * FROM advertisementsetentity WHERE id IN (:advertisementSetEntityIds)")
    fun loadAllByIds(advertisementSetEntityIds: IntArray): List<AdvertisementSetEntity>

    @Query("SELECT * FROM advertisementsetentity WHERE target = :target")
    fun findByTarget(target:AdvertisementTarget): List<AdvertisementSetEntity>

    @Query("SELECT * FROM advertisementsetentity WHERE type = :type")
    fun findByType(type:AdvertisementSetType): List<AdvertisementSetEntity>

    @Insert
    fun insertAll(vararg advertisementSetEntity: AdvertisementSetEntity)

    @Delete
    fun delete(advertisementSetEntity: AdvertisementSetEntity)

    @Insert
    fun insertItem(advertisementSetEntity: AdvertisementSetEntity): Long
}