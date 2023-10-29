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
            var params = AdvertisingSetParameters.Builder()

            params.setLegacyMode(legacyMode)
            params.setInterval(interval)
            params.setPrimaryPhy(primaryPhy)
            params.setSecondaryPhy(secondaryPhy)

            try{
                when (txPowerLevel) {
                    0 -> {
                        params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_ULTRA_LOW)
                    }
                    1 -> {
                        params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_LOW)
                    }
                    2 -> {
                        params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                    }
                    3 -> {
                        params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                    } else -> {
                        params.setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                    }
                }
            }catch (ex:IllegalArgumentException){
                Log.e(_logTag, "Could not execute setTxPowerLevel: ${ex.message}")
            }

            return params.build()
        } else {
            Log.d(_logTag, "AdvertisingSetParametersModel could not be built because its invalid")
        }
        return null
    }

}