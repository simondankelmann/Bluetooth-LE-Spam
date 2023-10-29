package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log

class AdvertiseSettingsModel {
    private var _logTag = "AdvertiseSettingsModel"

    var advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
    var txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
    var connectable = true
    var timeout = 0

    fun validate():Boolean{
        return true
    }
    fun build():AdvertiseSettings?{
        if(validate()){
            var settings = AdvertiseSettings.Builder()

            settings.setAdvertiseMode(advertiseMode)
            settings.setConnectable(connectable)
            settings.setTimeout(timeout)

            try {
                when (txPowerLevel) {
                    0 -> {
                        settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
                    }
                    1 -> {
                        settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                    }
                    2 -> {
                        settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    }
                    3 -> {
                        settings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    } else -> {
                    settings.setTxPowerLevel(txPowerLevel)
                }
                }
            } catch (ex:IllegalArgumentException){
                Log.e(_logTag, "Could not execute setTxPowerLevel: ${ex.message}")
            }


            return settings.build()
        } else {
            Log.d(_logTag, "AdvertiseSettingsModel could not be built because its invalid")
        }
        return null
    }
}