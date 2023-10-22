package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.PeriodicAdvertisingParameters

class AdvertisementSet {

    // BLE Data
    lateinit var advertisingSetParameters:AdvertisingSetParameters
    lateinit var advertiseData:AdvertiseData
    val scanResponse:AdvertiseData? = null
    var periodicParameters:PeriodicAdvertisingParameters? = null
    var periodicData:AdvertiseData? = null
    lateinit var callback:AdvertisingSetCallback

    // Custom Data
    var deviceName = ""

    fun validate():Boolean{
        //@todo: implement checks here
        return true
    }
}