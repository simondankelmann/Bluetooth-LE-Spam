package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AssociatonCollectionListEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "advertisementSetCollectionId") var advertisementSetCollectionId: Int,
    @ColumnInfo(name = "advertisementSetListId") var advertisementSetListId: Int,
    @ColumnInfo(name = "position") var position: Int,
)
