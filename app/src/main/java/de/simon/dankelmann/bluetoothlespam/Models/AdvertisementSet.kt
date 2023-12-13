package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertisingSetCallback
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import java.io.Serializable

class AdvertisementSet : Serializable {
    private val _logTag = "AdvertisementSet"

    // Data
    var id = 0
    var title = ""
    var target:AdvertisementTarget = AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED
    var type:AdvertisementSetType = AdvertisementSetType.ADVERTISEMENT_TYPE_UNDEFINED
    var duration:Int = 0
    var maxExtendedAdvertisingEvents:Int = 0
    var range:AdvertisementSetRange = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_UNKNOWN

    // Related Data
    var advertiseSettings: AdvertiseSettings = AdvertiseSettings()
    var advertisingSetParameters:AdvertisingSetParameters = AdvertisingSetParameters()
    var advertiseData:AdvertiseData = AdvertiseData()
    var scanResponse:AdvertiseData? = null
    var periodicAdvertisingParameters:PeriodicAdvertisingParameters? = null
    var periodicAdvertiseData:AdvertiseData? = null

    // Callbacks
    lateinit var advertisingSetCallback:AdvertisingSetCallback
    lateinit var advertisingCallback: AdvertiseCallback

    // Ui Data
    var currentlyAdvertising = false
    var advertisementState = AdvertisementState.ADVERTISEMENT_STATE_UNDEFINED

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