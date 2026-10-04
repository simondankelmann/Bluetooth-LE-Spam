package de.simon.dankelmann.bluetoothlespam.Callbacks

import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.util.Log


class GenericAdvertisingSetCallback() : AdvertisingSetCallback() {

    private val _logTag = "GenericAdvertisingSetCallback"

    override fun onAdvertisingSetStarted(
        advertisingSet: AdvertisingSet?,
        txPower: Int,
        status: Int
    ) {
        Log.d(_logTag, "onAdvertisingSetStarted(): txPower=$txPower status=$status")

        if (advertisingSet == null) {
            Log.d(_logTag, "advertising set is null");
        }
    }

    override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
        Log.d(_logTag, "onAdvertisingDataSet() status=$status")
    }

    override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
        Log.d(_logTag, "onScanResponseDataSet(): status=$status")
    }

    override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
        Log.d(_logTag, "onAdvertisingSetStopped():")
    }

}