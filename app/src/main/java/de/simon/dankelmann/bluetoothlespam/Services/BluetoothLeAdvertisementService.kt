package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeAdvertiser
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck

class BluetoothLeAdvertisementService (_bluetoothAdapter: BluetoothAdapter) {
    private val _logTag = "BluetoothLeAdvertisementService"
    private val _advertiser: BluetoothLeAdvertiser = _bluetoothAdapter.bluetoothLeAdvertiser

    init {
        if(_bluetoothAdapter == null){
            Log.e(_logTag, "Adapter is null")
        }

        if(_advertiser == null){
            Log.e(_logTag, "Advertiser is null")
        }
    }

    fun startAdvertising(advertisementSet: AdvertisementSet){
        if(advertisementSet.validate()){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                _advertiser.startAdvertisingSet(advertisementSet.advertisingSetParameters, advertisementSet.advertiseData, advertisementSet.scanResponse, advertisementSet.periodicParameters, advertisementSet.periodicData, advertisementSet.callback)
                Log.d(_logTag, "Executed advertisement Start")
            } else {
                Log.d(_logTag, "Missing permission to execute advertisement")
            }
        } else {
            Log.d(_logTag, "Advertisementset could not be validated")
        }

        /*
        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            //.setConnectable(true)
            //.setScannable(true)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            //.setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
            //.setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
            .build()

        val advertiseData: AdvertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(serviceUuid)
            .addServiceData(serviceUuid, serviceData)
            //.addManufacturerData(manufacturerId, serviceData)
            //.addManufacturerData(manufacturerId, manufacturerSpecificData)
            .setIncludeTxPowerLevel(true)
            .build()

        val periodicParameters: PeriodicAdvertisingParameters = PeriodicAdvertisingParameters.Builder()
            .setInterval(800)
            .setIncludeTxPower(true)
            .build()

        val scanResponse = AdvertiseData.Builder().addServiceUuid(serviceUuid).build()


        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
            _advertiser.startAdvertisingSet(parameters, advertiseData, null, null, null, advertiseSetCallback);
            Log.d(_logTag, "Executed advertisement Start")
        }*/
    }

    fun stopAdvertising(advertisementSet: AdvertisementSet){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
            _advertiser.stopAdvertisingSet(advertisementSet.callback)
            Log.d(_logTag, "Executed advertisement Stop")
        } else {
            Log.d(_logTag, "Missing permission to stop advertisement")
        }
    }

}