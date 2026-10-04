package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.PeriodicAdvertisingParametersEntity

@Dao
interface PeriodicAdvertisingParametersDao {
    @Query("SELECT * FROM periodicadvertisingparametersentity WHERE id = :id")
    fun findById(id: Int): PeriodicAdvertisingParametersEntity

    @Query("SELECT * FROM periodicadvertisingparametersentity")
    fun getAll(): List<PeriodicAdvertisingParametersEntity>

    @Insert
    fun insertAll(vararg periodicAdvertisingParametersEntity: PeriodicAdvertisingParametersEntity)

    @Delete
    fun delete(periodicAdvertisingParametersEntity: PeriodicAdvertisingParametersEntity)

    @Insert
    fun insertItem(periodicAdvertisingParametersEntity: PeriodicAdvertisingParametersEntity): Long
}