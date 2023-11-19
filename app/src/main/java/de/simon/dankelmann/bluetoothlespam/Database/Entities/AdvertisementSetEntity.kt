package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget

@Entity
data class AdvertisementSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,

    // Data
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "target") var target: AdvertisementTarget,
    @ColumnInfo(name = "type") var type: AdvertisementSetType,
    @ColumnInfo(name = "duration") var duration: Int,
    @ColumnInfo(name = "maxExtendedAdvertisingEvents") var maxExtendedAdvertisingEvents: Int,
    @ColumnInfo(name = "range") var range: AdvertisementSetRange,

    // Related Data
    @ColumnInfo(name = "advertiseSettingsId") var advertiseSettingsId: Int,
    @ColumnInfo(name = "advertisingSetParametersId") var advertisingSetParametersId: Int,
    @ColumnInfo(name = "advertiseDataId") var advertiseDataId: Int,
    @ColumnInfo(name = "scanResponseId") var scanResponseId: Int?,
    @ColumnInfo(name = "periodicAdvertisingParametersId") var periodicAdvertisingParametersId: Int?,
    @ColumnInfo(name = "periodicAdvertiseDataId") var periodicAdvertiseDataId: Int?,
)
