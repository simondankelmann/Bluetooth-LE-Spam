package de.simon.dankelmann.bluetoothlespam.Database.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociatonCollectionListEntity

data class CollectionWithLists(
    @Embedded val collection: AdvertisementSetCollectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AssociatonCollectionListEntity::class,
            parentColumn = "advertisementSetCollectionId",
            entityColumn = "advertisementSetListId",
        ),
    )
    val lists: List<AdvertisementSetListEntity>,
)

@Dao
interface AdvertisementSetCollectionDao {
    @Query("SELECT * FROM advertisementsetcollectionentity WHERE id = :id")
    fun findById(id: Int): AdvertisementSetCollectionEntity

    @Query("SELECT * FROM advertisementsetcollectionentity")
    fun getAll(): List<AdvertisementSetCollectionEntity>

    @Query("SELECT * FROM advertisementsetcollectionentity WHERE isCustom = 0")
    fun getBuiltInCollections(): List<AdvertisementSetCollectionEntity>

    @Query("SELECT * FROM advertisementsetcollectionentity WHERE isCustom = 1")
    fun getCustomCollections(): List<AdvertisementSetCollectionEntity>

    @Query("SELECT * FROM advertisementsetcollectionentity WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecentlyUsed(limit: Int = 3): List<AdvertisementSetCollectionEntity>

    @Query("SELECT * FROM advertisementsetcollectionentity WHERE isQuickStart = 1 ORDER BY quickStartOrder ASC")
    fun getQuickStartCollections(): List<AdvertisementSetCollectionEntity>

    @Query("SELECT MAX(quickStartOrder) FROM advertisementsetcollectionentity")
    fun getMaxQuickStartOrder(): Int?

    @Query("UPDATE advertisementsetcollectionentity SET isQuickStart = :isQuickStart, quickStartOrder = :order WHERE id = :id")
    fun setQuickStart(id: Int, isQuickStart: Boolean, order: Int?)

    @Transaction
    @Query("SELECT * FROM advertisementsetcollectionentity WHERE id = :id")
    fun getCollectionWithLists(id: Int): CollectionWithLists

    @Transaction
    @Query("SELECT * FROM advertisementsetcollectionentity ORDER BY title")
    fun getAllCollectionsWithLists(): List<CollectionWithLists>

    @Query("UPDATE advertisementsetcollectionentity SET lastUsedAt = :timestamp WHERE id = :id")
    fun updateLastUsedAt(id: Int, timestamp: Long)

    @Insert
    fun insertAll(vararg advertisementSetColletionEntity: AdvertisementSetCollectionEntity)

    @Delete
    fun delete(advertisementSetColletionEntity: AdvertisementSetCollectionEntity)

    @Insert
    fun insertItem(advertisementSetCollectionEntity: AdvertisementSetCollectionEntity): Long
}
