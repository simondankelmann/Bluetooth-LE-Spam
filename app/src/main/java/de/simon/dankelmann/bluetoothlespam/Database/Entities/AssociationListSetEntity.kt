package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** List<->set join (plan §8 activation) — either side being deleted cleans up the association. */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = AdvertisementSetListEntity::class,
            parentColumns = ["id"],
            childColumns = ["advertisementSetListId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AdvertisementSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["advertisementSetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("advertisementSetListId"),
        Index("advertisementSetId"),
    ],
)
data class AssociationListSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "advertisementSetId") var advertisementSetId: Int,
    @ColumnInfo(name = "advertisementSetListId") var advertisementSetListId: Int,
    @ColumnInfo(name = "position") var position: Int,
)
