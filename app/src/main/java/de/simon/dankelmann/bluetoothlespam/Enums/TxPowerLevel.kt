package de.simon.dankelmann.bluetoothlespam.Enums

import de.simon.dankelmann.bluetoothlespam.R

enum class TxPowerLevel {
    TX_POWER_HIGH,
    TX_POWER_MEDIUM,
    TX_POWER_LOW,
    TX_POWER_ULTRA_LOW
}

fun TxPowerLevel.toStringId(): Int {
    return when (this) {
        TxPowerLevel.TX_POWER_HIGH -> R.string.power_level_high
        TxPowerLevel.TX_POWER_MEDIUM -> R.string.power_level_medium
        TxPowerLevel.TX_POWER_LOW -> R.string.power_level_low
        TxPowerLevel.TX_POWER_ULTRA_LOW -> R.string.power_level_ultra_low
    }
}
