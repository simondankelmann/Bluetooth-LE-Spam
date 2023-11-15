package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget

@Entity
data class AdvertiseDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "includeDeviceName") var boolean: Boolean,
    @ColumnInfo(name = "includeTxPower") var includeTxPower: Boolean
)
