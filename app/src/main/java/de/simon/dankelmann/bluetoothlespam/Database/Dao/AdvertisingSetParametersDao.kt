package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertiseSettingsEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisingSetParametersEntity

@Dao
interface AdvertisingSetParametersDao {
    @Query("SELECT * FROM advertisingsetparametersentity WHERE id = :id")
    fun findById(id: Int): AdvertisingSetParametersEntity

    @Query("SELECT * FROM advertisingsetparametersentity")
    fun getAll(): List<AdvertisingSetParametersEntity>

    @Insert
    fun insertAll(vararg advertisingSetParametersEntity: AdvertisingSetParametersEntity)

    @Delete
    fun delete(advertisingSetParametersEntity: AdvertisingSetParametersEntity)

    @Insert
    fun insertItem(advertisingSetParametersEntity: AdvertisingSetParametersEntity): Long

    @Query("DELETE FROM advertisingsetparametersentity WHERE id = :id")
    fun deleteById(id: Int)

    @Query("SELECT * FROM advertisingsetparametersentity WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<AdvertisingSetParametersEntity>
}