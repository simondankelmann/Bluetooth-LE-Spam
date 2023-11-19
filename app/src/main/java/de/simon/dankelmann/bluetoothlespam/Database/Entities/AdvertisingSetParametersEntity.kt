package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.PrimaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.SecondaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel

@Entity
data class AdvertisingSetParametersEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,

    @ColumnInfo(name = "legacyMode") var legacyMode: Boolean,
    @ColumnInfo(name = "interval") var interval: Int,
    @ColumnInfo(name = "txPowerLevel") var txPowerLevel: TxPowerLevel,
    @ColumnInfo(name = "includeTxPowerLevel") var includeTxPowerLevel: Boolean,
    @ColumnInfo(name = "primaryPhy") var primaryPhy: PrimaryPhy?,
    @ColumnInfo(name = "secondaryPhy") var secondaryPhy: SecondaryPhy?,
    @ColumnInfo(name = "scanable") var scanable: Boolean,
    @ColumnInfo(name = "connectable") var connectable: Boolean,
    @ColumnInfo(name = "anonymous") var anonymous: Boolean,
)
