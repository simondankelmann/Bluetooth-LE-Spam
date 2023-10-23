package de.simon.dankelmann.bluetoothlespam.Callbacks

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.util.Log

class GoogleFastPairAdvertisingCallback : AdvertiseCallback() {
    
    private val _logTag = "GoogleFastPairAdvertisingCallback"
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
        super.onStartSuccess(settingsInEffect)
        Log.d(_logTag, "======= onStartSuccess:")
        Log.d(_logTag, settingsInEffect.toString())
    }

    override fun onStartFailure(errorCode: Int) {
        super.onStartFailure(errorCode)
        var description = ""
        description = if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
            "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
            "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
            "ADVERTISE_FAILED_ALREADY_STARTED"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE) {
            "ADVERTISE_FAILED_DATA_TOO_LARGE"
        } else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR) {
            "ADVERTISE_FAILED_INTERNAL_ERROR"
        } else {
            "unknown"
        }
        Log.d(_logTag, "error: $description")
    }
}