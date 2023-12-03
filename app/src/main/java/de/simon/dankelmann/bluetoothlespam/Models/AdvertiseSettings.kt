package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseSettings
import android.util.Log
import androidx.room.ColumnInfo
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import java.io.Serializable

class AdvertiseSettings : Serializable {
    private var _logTag = "AdvertiseSettingsModel"
    var id:Int = 0
    var advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
    var txPowerLevel = TxPowerLevel.TX_POWER_HIGH
    var connectable = true
    var timeout = 0

    fun validate():Boolean{
        return true
    }
    fun build(): AdvertiseSettings?{
        if(validate()){
            var settings = AdvertiseSettings.Builder()

            settings.setConnectable(connectable)
            settings.setTimeout(timeout)

            when (advertiseMode){
                AdvertiseMode.ADVERTISEMODE_LOW_LATENCY -> {
                    settings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                }
                AdvertiseMode.ADVERTISEMODE_LOW_POWER -> {
                    settings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                }

                AdvertiseMode.ADVERTISEMODE_BALANCED -> {
                    settings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                }
            }

            when (txPowerLevel) {
                TxPowerLevel.TX_POWER_ULTRA_LOW -> {
                    settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
                }
                TxPowerLevel.TX_POWER_LOW -> {
                    settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                }
                TxPowerLevel.TX_POWER_MEDIUM -> {
                    settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                }
                TxPowerLevel.TX_POWER_HIGH -> {
                    settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                }
            }

            return settings.build()
        } else {
            Log.d(_logTag, "AdvertiseSettings could not be built because its invalid")
        }
        return null
    }
}