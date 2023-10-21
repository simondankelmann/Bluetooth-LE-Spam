package de.simonkelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.PeriodicAdvertisingParameters
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import de.simonkelmann.bluetoothlespam.AppContext.AppContext
import de.simonkelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import java.util.Dictionary
import java.util.UUID


class BleService (_bluetoothAdapter: BluetoothAdapter){

    private val _logTag = "BleService"
    private val _advertiser: BluetoothLeAdvertiser = _bluetoothAdapter.bluetoothLeAdvertiser

    val serviceUuid = ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))
    val serviceData = "CD8256".decodeHex()//byteArrayOf("DC".toInt(16).toByte(), 0xE9, 0xEA)


    /*

    {0xCD8256, "Bose NC 700"},
    {0xF52494, "JBL Buds Pro"},
    {0x718FA4, "JBL Live 300TWS"},
    {0x821F66, "JBL Flip 6"},
    {0x92BBBD, "Pixel Buds"},
    {0xD446A7, "Sony XM5"},
    {0x2D7A23, "Sony WF-1000XM4"},
    {0x0E30C3, "Razer Hammerhead TWS"},
    {0x72EF8D, "Razer Hammerhead TWS X"},
    {0x72FB00, "Soundcore Spirit Pro GVA"},
   
    */

    var manufacturerId = 0x009E// 0xFE21 => 65057 => BOSE
    var manufacturerSpecificData:ByteArray = byteArrayOf() //byteArrayOf(0x1e, 0xff.toByte(), 0x4c, 0x00, 0x07, 0x19, 0x07, 0x02, 0x20, 0x75, 0xaa.toByte(), 0x30, 0x01, 0x00, 0x00, 0x45, 0x12, 0x12, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

    init {
        if(_bluetoothAdapter == null){
            Log.e(_logTag, "Adapter is null")
        }

        if(_advertiser == null){
            Log.e(_logTag, "Advertiser is null")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startAdvertising(){

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
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stopAdvertising(){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
            _advertiser.stopAdvertisingSet(advertiseSetCallback)
            Log.d(_logTag, "Executed advertisement Start")
        }
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val advertiseSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            val context = AppContext.getActivity()

            if (status==AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED)
                Toast.makeText(context, "ADVERTISE_FAILED_ALREADY_STARTED", Toast.LENGTH_SHORT).show();
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                Toast.makeText(context, "ADVERTISE_FAILED_FEATURE_UNSUPPORTED", Toast.LENGTH_SHORT).show();
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE)
                Toast.makeText(context, "ADVERTISE_FAILED_DATA_TOO_LARGE", Toast.LENGTH_SHORT).show();
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
                Toast.makeText(context, "ADVERTISE_FAILED_INTERNAL_ERROR", Toast.LENGTH_SHORT).show();
            else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                Toast.makeText(context, "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS", Toast.LENGTH_SHORT).show();
            else if (status==AdvertisingSetCallback.ADVERTISE_SUCCESS)
                Toast.makeText(context, "ADVERTISE_SUCCESS", Toast.LENGTH_SHORT).show();

            Log.i(_logTag, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: " + status)

            if(advertisingSet != null){
                if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                    //advertisingSet!!.setScanResponseData(AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.randomUUID())).build())
                    Log.d(_logTag,"added scanresponse data")
                }
            } else {
                Log.d(_logTag,"advertising set is null");
            }
        }

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
            Log.i(_logTag, "onAdvertisingDataSet() :status:$status")
        }

        override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
            Log.i(_logTag, "onScanResponseDataSet(): status:$status")
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
            Log.i(_logTag, "onAdvertisingSetStopped():")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(_logTag, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(_logTag, "LE Advertise Failed: $errorCode")
        }
    }


    /*
    private var _currentAdvertisingSet:AdvertisingSet? = null
    private var _macAddress = ""
    private val _callback: AdvertisingSetCallback = @RequiresApi(Build.VERSION_CODES.O)

    object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(
            advertisingSet: AdvertisingSet?,
            txPower: Int,
            status: Int
        ) {
            Log.i(
                _logTag, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: " + status
            )

            _currentAdvertisingSet = advertisingSet
        }

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
            Log.i(_logTag, "onAdvertisingDataSet() :status:$status")
        }

        override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
            Log.i(_logTag, "onScanResponseDataSet(): status:$status")
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
            Log.i(_logTag, "onAdvertisingSetStopped():")
        }
    }


    init {
        if(_bluetoothAdapter == null){
            Log.d(_logTag, "Adapter is null !!!")
        } else {
            Log.d(_logTag, "Adapter is not null")
        }

        if(!_bluetoothAdapter.isMultipleAdvertisementSupported){
            Log.d(_logTag, "isMultipleAdvertisementSupported is false !!!")
        } else {
            Log.d(_logTag, "isMultipleAdvertisementSupported is true")
        }

    }


    // apple = 004C => 76
    /*
    var manufacturerId = 76
    var manufacturerSpecificData:ByteArray = byteArrayOf(0x1e,
        0xff.toByte(), 0x4c, 0x00, 0x07, 0x19, 0x07, 0x02, 0x20, 0x75, 0xaa.toByte(), 0x30, 0x01, 0x00, 0x00, 0x45, 0x12, 0x12, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    */

    var manufacturerId = 0xFE21 // 0xFE21 => BOSE
    var manufacturerSpecificData:ByteArray = byteArrayOf() //byteArrayOf(0x1e, 0xff.toByte(), 0x4c, 0x00, 0x07, 0x19, 0x07, 0x02, 0x20, 0x75, 0xaa.toByte(), 0x30, 0x01, 0x00, 0x00, 0x45, 0x12, 0x12, 0x12, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

    val serviceUuid = ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))
    val serviceData = "DATA".getBytes() //byteArrayOf() //"DCE9EA".decodeHex()//byteArrayOf("DC".toInt(16).toByte(), 0xE9, 0xEA)

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stopAdvertising(){
        // When done with the advertising:
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())) {
            Log.d(_logTag, "Stopping Advertiser")
            _advertiser.stopAdvertisingSet(_callback)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun advertise(){
        Log.d(_logTag, "Calling Function")

        //val advertiser: BluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser

        /*
        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(false) // True by default, but set here as a reminder.
            .setConnectable(true)
            //.setScannable(true)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
            .build()*/

        val parameters = AdvertisingSetParameters.Builder()
            .setLegacyMode(true)
            .setConnectable(true)
            .setScannable(true)
            .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
            .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
            .setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
            .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
            .build()

        /*
        val data = AdvertiseData.Builder()
            .addServiceUuid(serviceUuid)
            .addServiceData(serviceUuid, serviceData)
            .setIncludeDeviceName(true)
            //.addManufacturerData(manufacturerId, manufacturerSpecificData)
            .build()*/

        val advertiseData: AdvertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(serviceUuid)
            .addServiceData(serviceUuid, serviceData)
            .build()

        val periodicParameters: PeriodicAdvertisingParameters = PeriodicAdvertisingParameters.Builder()
            .setInterval(800)
            .build()


        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
            _advertiser.startAdvertisingSet(parameters, advertiseData, null, periodicParameters, null, _callback);
        }

        // After onAdvertisingSetStarted callback is called, you can modify the
        // advertising data and scan response data:
        // After onAdvertisingSetStarted callback is called, you can modify the
        // advertising data and scan response data:
        if(_currentAdvertisingSet != null){
            _currentAdvertisingSet!!.setAdvertisingData(
                AdvertiseData.Builder().setIncludeDeviceName(true).setIncludeTxPowerLevel(true).build()
            )
        } else {
            Log.d(_logTag, "CurrentAdvertisingSet not yet initialized 1");
        }


        // Wait for onAdvertisingDataSet callback...
        // Wait for onAdvertisingDataSet callback...
        if(_currentAdvertisingSet != null){
            _currentAdvertisingSet!!.setScanResponseData(
                AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.randomUUID())).build()
            )
        } else {
            Log.d(_logTag, "CurrentAdvertisingSet not yet initialized 2");
        }

        // Wait for onScanResponseDataSet callback...

        // When done with the advertising:
        // Wait for onScanResponseDataSet callback...



    }*/
}