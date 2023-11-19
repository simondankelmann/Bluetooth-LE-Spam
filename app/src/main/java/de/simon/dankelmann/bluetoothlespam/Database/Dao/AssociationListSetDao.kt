package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociationListSetEntity

@Dao
interface AssociationListSetDao {
    @Query("SELECT * FROM associationlistsetentity WHERE id = :id")
    fun findById(id: Int): AssociationListSetEntity

    @Query("SELECT * FROM associationlistsetentity")
    fun getAll(): List<AssociationListSetEntity>

    @Insert
    fun insertAll(vararg associationListSetEntity: AssociationListSetEntity)

    @Delete
    fun delete(associationListSetEntity: AssociationListSetEntity)

    @Insert
    fun insertItem(associationListSetEntity: AssociationListSetEntity): Long
}