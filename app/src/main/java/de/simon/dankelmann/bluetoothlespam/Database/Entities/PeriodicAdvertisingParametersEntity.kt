package de.simon.dankelmann.bluetoothlespam.Database.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PeriodicAdvertisingParametersEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "includeTxPowerLevel") var includeTxPowerLevel: Boolean,
    @ColumnInfo(name = "interval") var interval: Int
)
