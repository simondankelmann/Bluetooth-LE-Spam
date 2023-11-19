package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget

@Entity
data class AdvertiseDataEntity(
    @PrimaryKey(autoGenerate = true) var id: Int,

    @ColumnInfo(name = "includeDeviceName") var includeDeviceName: Boolean,
    @ColumnInfo(name = "includeTxPower") var includeTxPower: Boolean
){
    constructor():this(0,false,false)
}