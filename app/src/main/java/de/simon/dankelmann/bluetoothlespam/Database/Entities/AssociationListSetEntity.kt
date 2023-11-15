package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AssociationListSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "advertisementSetId") var advertisementSetId: Int,
    @ColumnInfo(name = "advertisementSetListId") var advertisementSetListId: Int,
    @ColumnInfo(name = "position") var position: Int,
)
