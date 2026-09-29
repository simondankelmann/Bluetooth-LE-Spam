package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Collection<->list join (plan §8) — deleting a collection cleans up its own join rows
 * (CASCADE), but deleting a list never cascades into deleting the collections referencing it
 * (NO_ACTION) — a list can be shared by multiple collections (e.g. Kitchen Sink reuses every
 * other built-in collection's lists).
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AdvertisementSetCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["advertisementSetCollectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AdvertisementSetListEntity::class,
            parentColumns = ["id"],
            childColumns = ["advertisementSetListId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index("advertisementSetCollectionId"),
        Index("advertisementSetListId"),
    ],
)
data class AssociatonCollectionListEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "advertisementSetCollectionId") var advertisementSetCollectionId: Int,
    @ColumnInfo(name = "advertisementSetListId") var advertisementSetListId: Int,
    @ColumnInfo(name = "position") var position: Int,
)
