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

    @Query("SELECT * FROM associationlistsetentity WHERE advertisementSetListId = :listId ORDER BY position")
    fun findByListId(listId: Int): List<AssociationListSetEntity>

    @Insert
    fun insertAll(vararg associationListSetEntity: AssociationListSetEntity)

    @Delete
    fun delete(associationListSetEntity: AssociationListSetEntity)

    @Insert
    fun insertItem(associationListSetEntity: AssociationListSetEntity): Long

    @Query("SELECT * FROM associationlistsetentity WHERE advertisementSetId = :setId")
    fun findBySetId(setId: Int): List<AssociationListSetEntity>

    @Query("DELETE FROM associationlistsetentity WHERE advertisementSetId = :setId")
    fun deleteBySetId(setId: Int)

    @Query("SELECT MAX(position) FROM associationlistsetentity WHERE advertisementSetListId = :listId")
    fun getMaxPositionForList(listId: Int): Int?
}