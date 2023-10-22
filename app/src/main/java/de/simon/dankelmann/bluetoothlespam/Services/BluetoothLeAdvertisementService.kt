package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Callbacks.GoogleFastPairAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import java.util.UUID


class BluetoothLeAdvertisementService (_bluetoothAdapter: BluetoothAdapter) {
    private val _bluetoothAdapter = _bluetoothAdapter
    private val _logTag = "BluetoothLeAdvertisementService"
    private val _advertiser: BluetoothLeAdvertiser = _bluetoothAdapter.bluetoothLeAdvertiser

    init {
        checkHardware()
        //startLeAdvertise(_bluetoothAdapter)
    }

    fun checkHardware():Boolean{

        if(_bluetoothAdapter == null){
            Log.e(_logTag, "Adapter is null")
            return false
        }

        if(!_bluetoothAdapter.isMultipleAdvertisementSupported){
            Log.e(_logTag, "Adapter does not support isMultipleAdvertisementSupported")
            return false
        }

        if(!_bluetoothAdapter.isOffloadedFilteringSupported){
            Log.e(_logTag, "Adapter does not support isOffloadedFilteringSupported")
            return false
        }

        if(!_bluetoothAdapter.isOffloadedScanBatchingSupported){
            Log.e(_logTag, "Adapter does not support isOffloadedScanBatchingSupported")
            return false
        }

        if(_advertiser == null){
            Log.e(_logTag, "Advertiser is null")
            return false
        }

        return true
    }


    private fun startLeAdvertise(bluetoothAdapter: BluetoothAdapter) {

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(Constants.ADVERTISE_TIMEOUT)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.fromString(Constants.UUID_GOOGLE_FAST_PAIRING)))
            .build()

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
            mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback)
        }
    }

    fun startAdvertisingSet(advertisementSet: AdvertisementSet){
        if(advertisementSet.validate()){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                _advertiser.startAdvertisingSet(advertisementSet.advertisingSetParameters, advertisementSet.advertiseData, advertisementSet.scanResponse, advertisementSet.periodicParameters, advertisementSet.periodicData, advertisementSet.callback)
                //Log.d(_logTag, "Executed advertisement Start")
            } else {
                Log.d(_logTag, "Missing permission to execute advertisement")
            }
        } else {
            Log.d(_logTag, "Advertisementset could not be validated")
        }
    }

    fun stopAdvertisingSet(advertisementSet: AdvertisementSet){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
            _advertiser.stopAdvertisingSet(advertisementSet.callback)
            //Log.d(_logTag, "Executed advertisement Stop")
        } else {
            Log.d(_logTag, "Missing permission to stop advertisement")
        }
    }

}