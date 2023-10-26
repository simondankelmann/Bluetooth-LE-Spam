package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log

class AdvertisingSetParametersModel {
    private var _logTag = "AdvertisingSetParametersModel"


    var legacyMode = true
    var interval = AdvertisingSetParameters.INTERVAL_MIN
    var txPowerLevel = AdvertisingSetParameters.TX_POWER_HIGH
    var primaryPhy = BluetoothDevice.PHY_LE_CODED
    var secondaryPhy = BluetoothDevice.PHY_LE_2M

    fun validate():Boolean{
        //@Todo: implement validation here
        return true
    }
    fun build(): AdvertisingSetParameters? {
        if(validate()){
            return AdvertisingSetParameters.Builder()
                .setLegacyMode(legacyMode)
                .setInterval(interval)
                .setTxPowerLevel(txPowerLevel)
                .setPrimaryPhy(primaryPhy)
                .setSecondaryPhy(secondaryPhy)
                .build()
        } else {
            Log.d(_logTag, "AdvertisingSetParametersModel could not be built because its invalid")
        }
        return null
    }

}