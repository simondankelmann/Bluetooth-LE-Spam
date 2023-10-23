package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.PeriodicAdvertisingParameters

class AdvertisementSet {

    // BLE Data
    lateinit var advertiseSettings: AdvertiseSettings
    lateinit var advertisingSetParameters:AdvertisingSetParameters
    lateinit var advertiseData:AdvertiseData
    var scanResponse:AdvertiseData? = null
    var periodicParameters:PeriodicAdvertisingParameters? = null
    var periodicData:AdvertiseData? = null
    lateinit var advertisingSetCallback:AdvertisingSetCallback
    lateinit var advertisingCallback: AdvertiseCallback

    // Custom Data
    var deviceName = ""

    fun validate():Boolean{
        //@todo: implement checks here
        return true
    }
}