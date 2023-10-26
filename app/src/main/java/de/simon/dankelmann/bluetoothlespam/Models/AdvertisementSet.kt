package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.PeriodicAdvertisingParameters
import android.util.Log

class AdvertisementSet {
    private val _logTag = "AdvertisementSet"

    // BLE Data Models
    var advertiseSettings: AdvertiseSettingsModel = AdvertiseSettingsModel()
    var advertisingSetParameters:AdvertisingSetParametersModel = AdvertisingSetParametersModel()
    var advertiseData:AdvertiseDataModel = AdvertiseDataModel()
    var scanResponse:AdvertiseDataModel = AdvertiseDataModel()
    //var periodicParameters:PeriodicAdvertisingParameters? = null
    //var periodicData:AdvertiseDataModel = AdvertiseDataModel()

    // Callbacks
    lateinit var advertisingSetCallback:AdvertisingSetCallback
    lateinit var advertisingCallback: AdvertiseCallback

    // Custom Data
    var deviceName = ""

    fun validate():Boolean{
        //@todo: implement checks here
        return true
    }

    fun build(){
        if(validate()){

        } else {
            Log.d(_logTag, "Advertisement set could ne be built because it is invalid")
        }
    }
}