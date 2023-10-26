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
        //@Todo: implement validation here
        return true
    }
    fun build():AdvertiseSettings?{
        if(validate()){
            return AdvertiseSettings.Builder()
                .setAdvertiseMode(advertiseMode)
                .setTxPowerLevel(txPowerLevel)
                .setConnectable(connectable)
                .setTimeout(timeout)
                .build()
        } else {
            Log.d(_logTag, "AdvertiseSettingsModel could not be built because its invalid")
        }
        return null
    }
}