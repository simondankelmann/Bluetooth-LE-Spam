package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.GoogleFastPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import java.util.UUID

class BleAdvertisementService {

    private val _logTag = "BleAdvertisementService"
    private var _bleAdvertisementServiceCallback:MutableList<IBleAdvertisementServiceCallback> = mutableListOf()

     fun startLeAdvertise(bluetoothAdapter: BluetoothAdapter) {

        val gen = GoogleFastPairAdvertisementSetGenerator()
        var items = gen.getAdvertisementSets()
        var firstItem = items[0]

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(1600)
            .build()

         val advertiseData: AdvertiseData = AdvertiseData.Builder()
             .setIncludeDeviceName(false)
             .addServiceUuid(ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")))
             .addServiceData(ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")), StringHelpers.decodeHex("821F66"))
             //.addManufacturerData(manufacturerId, serviceData)
             //.addManufacturerData(manufacturerId, manufacturerSpecificData)
             .setIncludeTxPowerLevel(true)
             .build()

        /*
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString(Constants.UUID_GOOGLE_FAST_PAIRING)))
            .build()*/

        //val data = firstItem.advertiseData

        var mAdvertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)
                Log.i(_logTag, "======= onStartSuccess:")
                Log.i(_logTag, settingsInEffect.toString())
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                var description = ""
                description = if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                    "ADVERTISE_FAILED_FEATURE_UNSUPPORTED"
                } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                    "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS"
                } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                    "ADVERTISE_FAILED_ALREADY_STARTED"
                } else if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                    "ADVERTISE_FAILED_DATA_TOO_LARGE"
                } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                    "ADVERTISE_FAILED_INTERNAL_ERROR"
                } else {
                    "unknown"
                }
                Log.i(_logTag, "error: $description")
            }
        }
        val mBluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())) {
            mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, mAdvertiseCallback)
            //mBluetoothLeAdvertiser.startAdvertisingSet(firstItem.advertisingSetParameters, advertiseData, null, null, null, firstItem.callback )
        }
    }

    fun addBleAdvertisementServiceCallback(callback: IBleAdvertisementServiceCallback){
        _bleAdvertisementServiceCallback.add(callback)
    }

}