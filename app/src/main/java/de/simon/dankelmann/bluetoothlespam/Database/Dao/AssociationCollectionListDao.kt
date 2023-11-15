package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisingSetParametersEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociatonCollectionListEntity

@Dao
interface AssociationCollectionListDao {
    @Query("SELECT * FROM associatoncollectionlistentity WHERE id = :id")
    fun findById(id: Int): AssociatonCollectionListEntity

    @Query("SELECT * FROM associatoncollectionlistentity")
    fun getAll(): List<AssociatonCollectionListEntity>

    @Insert
    fun insertAll(vararg associatonCollectionListEntity: AssociatonCollectionListEntity)

    @Delete
    fun delete(associatonCollectionListEntity: AssociatonCollectionListEntity)
    @Insert
    fun insertItem(associatonCollectionListEntity: AssociatonCollectionListEntity): Long

}