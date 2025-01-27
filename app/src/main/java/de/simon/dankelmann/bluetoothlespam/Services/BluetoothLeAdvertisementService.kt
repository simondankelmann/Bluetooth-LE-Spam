package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R


class BluetoothLeAdvertisementService(
    _bluetoothAdapter: BluetoothAdapter,
    private val context: Context,
) {

    // private
    private val _bluetoothAdapter = _bluetoothAdapter
    private val _logTag = "BluetoothLeAdvertisementService"
    private var _currentAdvertisementSet:AdvertisementSet? = null
    private var _advertiser: BluetoothLeAdvertiser? = null
    private var _bleAdvertisementServiceCallback:MutableList<IBleAdvertisementServiceCallback> = mutableListOf()

    // public
    var includeDeviceName:Boolean? = null
    var txPowerLevel:TxPowerLevel? = null

    init {

        val advertiser = _bluetoothAdapter.bluetoothLeAdvertiser
        if(advertiser != null){
            _advertiser = advertiser
        } else {
            Log.e(_logTag, "Bluetooth Low Energy Advertiser could not be accessed")
        }

        checkHardware()
    }

    private fun useAdvertisingWithSettings():Boolean{
        val preferences = PreferenceManager.getDefaultSharedPreferences(context).all
        val prefKey = context.resources.getString(R.string.preference_key_use_legacy_advertising)
        preferences.forEach {
            if(it.key == prefKey){
                val useAdvertisingWithSettings = it.value as Boolean
                return !useAdvertisingWithSettings
            }
        }

        return false
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

        // BLUETOOTH 5 COMPATIBLE
        return true
    }

    private fun prepareAdvertisementSet(advertisementSet: AdvertisementSet): AdvertisementSet{
        if(txPowerLevel != null){
            advertisementSet.advertiseSettings.txPowerLevel = txPowerLevel!!
            advertisementSet.advertisingSetParameters.txPowerLevel = txPowerLevel!!
        }

        if(includeDeviceName != null){
            Log.d(_logTag, "Setting include Devicename to: ${includeDeviceName}")
            advertisementSet.advertiseData.includeDeviceName = includeDeviceName!!
        }

        return advertisementSet
    }

    fun advertiseSet(advertisementSet: AdvertisementSet){
        _currentAdvertisementSet = advertisementSet
        if(useAdvertisingWithSettings()){
            // BLUETOOTH 4 AND 5
            startAdvertising(advertisementSet)
        } else {
            // BLUETOOTH 5 ONLY
            startAdvertisingSet(advertisementSet)
        }
    }
    fun stopAdvertiseSet(advertisementSet: AdvertisementSet){
        if(useAdvertisingWithSettings()){
            stopAdvertising(advertisementSet)
        } else {
            stopAdvertisingSet(advertisementSet)
        }
        _currentAdvertisementSet = null
    }

    private fun startAdvertising(advertisementSet: AdvertisementSet){
        if(_advertiser != null){
            if (advertisementSet.validate()) {
                if (PermissionCheck.checkPermission(
                        Manifest.permission.BLUETOOTH_ADVERTISE, context
                    )
                ) {
                    val preparedAdvertisementSet = prepareAdvertisementSet(advertisementSet)
                    _advertiser!!.startAdvertising(preparedAdvertisementSet.advertiseSettings.build(), preparedAdvertisementSet.advertiseData.build(), preparedAdvertisementSet.advertisingCallback)
                    _bleAdvertisementServiceCallback.map {
                        it.onAdvertisementSetStarted(advertisementSet)
                    }
                } else {
                    Log.d(_logTag, "Missing permission to execute advertisement")
                }
            } else {
                Log.d(_logTag, "Advertisementset could not be validated")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    private fun stopAdvertising(advertisementSet: AdvertisementSet) {
        if (_advertiser != null) {
            if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, context)) {
                _advertiser!!.stopAdvertising(advertisementSet.advertisingCallback)
            } else {
                Log.d(_logTag, "Missing permission to stop advertisement")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    private fun startAdvertisingSet(advertisementSet: AdvertisementSet){
        if(_advertiser != null){
            if (advertisementSet.validate()) {
                if (PermissionCheck.checkPermission(
                        Manifest.permission.BLUETOOTH_ADVERTISE, context
                    )
                ) {
                    val preparedAdvertisementSet = prepareAdvertisementSet(advertisementSet)
                    _advertiser!!.startAdvertisingSet(preparedAdvertisementSet.advertisingSetParameters.build(), preparedAdvertisementSet.advertiseData.build(), null, null, null, preparedAdvertisementSet.advertisingSetCallback)
                    _bleAdvertisementServiceCallback.map {
                        it.onAdvertisementSetStarted(advertisementSet)
                    }
                } else {
                    Log.d(_logTag, "Missing permission to execute advertisement")
                }
            } else {
                Log.d(_logTag, "Advertisementset could not be validated")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    private fun stopAdvertisingSet(advertisementSet: AdvertisementSet) {
        if (_advertiser != null) {
            if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, context)) {
                _advertiser!!.stopAdvertisingSet(advertisementSet.advertisingSetCallback)
                _bleAdvertisementServiceCallback.map {
                    it.onAdvertisementStopped()
                }
            } else {
                Log.d(_logTag, "Missing permission to stop advertisement")
            }
        } else {
            Log.d(_logTag, "Advertiser is null")
        }
    }

    fun addBleAdvertisementServiceCallback(callback: IBleAdvertisementServiceCallback){
        _bleAdvertisementServiceCallback.add(callback)
    }
}