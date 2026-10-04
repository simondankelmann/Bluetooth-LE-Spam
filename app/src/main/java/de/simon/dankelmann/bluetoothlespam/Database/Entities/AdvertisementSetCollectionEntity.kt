package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AdvertisementSetCollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "lastUsedAt") var lastUsedAt: Long? = null,
    @ColumnInfo(name = "isCustom", defaultValue = "0") var isCustom: Boolean = false,
    @ColumnInfo(name = "isQuickStart", defaultValue = "0") var isQuickStart: Boolean = false,
    @ColumnInfo(name = "quickStartOrder") var quickStartOrder: Int? = null,
)
