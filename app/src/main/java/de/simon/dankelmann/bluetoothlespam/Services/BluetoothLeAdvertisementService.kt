package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothManager
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

    fun startAdvertising(advertisementSet: AdvertisementSet){
        if(advertisementSet.validate()){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                _advertiser.startAdvertising(advertisementSet.advertiseSettings, advertisementSet.advertiseData, advertisementSet.advertisingCallback)
            } else {
                Log.d(_logTag, "Missing permission to execute advertisement")
            }
        } else {
            Log.d(_logTag, "Advertisementset could not be validated")
        }
    }

    fun startAdvertisingSet(advertisementSet: AdvertisementSet){
        if(advertisementSet.validate()){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                _advertiser.startAdvertisingSet(advertisementSet.advertisingSetParameters, advertisementSet.advertiseData, advertisementSet.scanResponse, advertisementSet.periodicParameters, advertisementSet.periodicData, advertisementSet.advertisingSetCallback)
            } else {
                Log.d(_logTag, "Missing permission to execute advertisement")
            }
        } else {
            Log.d(_logTag, "Advertisementset could not be validated")
        }
    }

    fun stopAdvertisingSet(advertisementSet: AdvertisementSet){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
            _advertiser.stopAdvertisingSet(advertisementSet.advertisingSetCallback)
        } else {
            Log.d(_logTag, "Missing permission to stop advertisement")
        }
    }

    fun stopAdvertising(advertisementSet: AdvertisementSet){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
            _advertiser.stopAdvertising(advertisementSet.advertisingCallback)
        } else {
            Log.d(_logTag, "Missing permission to stop advertisement")
        }
    }

}