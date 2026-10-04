package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothLeDeviceClassificationHelper
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import java.time.Duration
import java.time.LocalDateTime

class BluetoothLeScanService(
    private val context: Context,
) : IBluetoothLeScanService, ScanCallback() {

    private val _logTag = "BluetoothLeScanService"
    private var _bluetoothAdapter:BluetoothAdapter? = null
    private var _bluetoothLeScanner:BluetoothLeScanner? = null
    private var _scanning = false
    private var _bluetoothLeScanServiceCallbacks:MutableList<IBluetoothLeScanCallback> = mutableListOf()
    private var _flipperDevicesList = mutableListOf<FlipperDeviceScanResult>()
    private var _spamPackageScanResultList = mutableListOf<SpamPackageScanResult>()

    private val _millis_housekeeping_flipper_devices:Long = 1000
    private val _millis_housekeeping_spam_packages:Long = 1000
    private val _millis_spam_package_lifetime = 5000
    private val _millis_flipper_device_lifetime = 5000

    init {
        _bluetoothAdapter = context.bluetoothAdapter()
        if(_bluetoothAdapter != null){
            _bluetoothLeScanner = _bluetoothAdapter!!.bluetoothLeScanner
        }

        // Start Housekeeping:
        startFlipperDevicesHouseKeeping()
        startSpamPackagesHouseKeeping()
        // Add Test Devices:
        /*
        for(i in 1..15){
            var f0Device = FlipperDeviceScanResult()
            f0Device.flipperDeviceType = FlipperDeviceType.FLIPPER_ZERO_BLACK
            f0Device.deviceName = "TestItem: ${i}"
            f0Device.address = i.toString()

            // Dont let it disappear
            f0Device.lastSeen = LocalDateTime.now().plusWeeks(1)

            _flipperDevicesList.add(f0Device)
        }
        */
    }

    // Cleanup Flipper Device List
    fun startFlipperDevicesHouseKeeping(){
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                cleanFlipperDevicesList()
                mainHandler.postDelayed(this, _millis_housekeeping_flipper_devices)
            }
        })
    }

    // Cleanup Flipper Device List
    fun startSpamPackagesHouseKeeping(){
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                cleanSpamPackageList()
                mainHandler.postDelayed(this, _millis_housekeeping_spam_packages)
            }
        })
    }

    private fun cleanFlipperDevicesList(){
        val currentTime = LocalDateTime.now()
        var itemsToRemove = mutableListOf<FlipperDeviceScanResult>()
        _flipperDevicesList.forEachIndexed{ index, flipperDevice ->
            val lifetime = Duration.between(flipperDevice.lastSeen, currentTime).toMillis()
            if(lifetime >= _millis_flipper_device_lifetime){
                itemsToRemove.add(flipperDevice)
            }
        }

        if(itemsToRemove.count() > 0){
            itemsToRemove.forEach{itemToRemove ->
                _flipperDevicesList.remove(itemToRemove)
            }
            _bluetoothLeScanServiceCallbacks.forEach{callback ->
                callback.onFlipperListUpdated()
            }
        }
    }

    private fun cleanSpamPackageList(){
        val currentTime = LocalDateTime.now()
        var itemsToRemove = mutableListOf<SpamPackageScanResult>()
        _spamPackageScanResultList.forEachIndexed{ index, spamPackage ->
            val lifetime = Duration.between(spamPackage.lastSeen, currentTime).toMillis()
            if(lifetime >= _millis_spam_package_lifetime){
                itemsToRemove.add(spamPackage)
            }
        }

        if(itemsToRemove.count() > 0){
            itemsToRemove.forEach{itemToRemove ->
                _spamPackageScanResultList.remove(itemToRemove)
            }
            _bluetoothLeScanServiceCallbacks.forEach{callback ->
                callback.onSpamResultPackageListUpdated()
            }
        }
    }



    override fun getFlipperDevicesList():MutableList<FlipperDeviceScanResult>{
        return _flipperDevicesList
    }

    override fun getSpamPackageScanResultList():MutableList<SpamPackageScanResult>{
        return _spamPackageScanResultList
    }

    override fun addBluetoothLeScanServiceCallback(callback: IBluetoothLeScanCallback){
        if(!_bluetoothLeScanServiceCallbacks.contains(callback)){
            _bluetoothLeScanServiceCallbacks.add(callback)
        }
    }
    override fun removeBluetoothLeScanServiceCallback(callback: IBluetoothLeScanCallback){
        if(_bluetoothLeScanServiceCallbacks.contains(callback)){
            _bluetoothLeScanServiceCallbacks.remove(callback)
        }
    }

    override fun isScanning():Boolean{
        return _scanning
    }

    override fun startScanning(){
        if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_SCAN, context)) {
            if (_bluetoothLeScanner != null) {
                // SET THE FILTERS AND SETTINGS
                val filterList:List<ScanFilter> = mutableListOf(ScanFilter.Builder().build())

                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                _bluetoothLeScanner!!.startScan(filterList, settings, this)

                Log.d(_logTag, "Started BLE Scan")
                _scanning = true
            }
        }
    }

    override fun stopScanning() {
        if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_SCAN, context)) {
            if (_bluetoothLeScanner != null) {
                _bluetoothLeScanner!!.stopScan(this)
                Log.d(_logTag, "Stopped BLE Scan")
                _scanning = false
            }
        }
    }

    // android.bluetooth.le.scancallback:
    override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        super.onBatchScanResults(results)
        Log.d(_logTag, "onBatchScanResults called")
    }


    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        super.onScanResult(callbackType, result)
        if(result != null){
            val bluetoothLeScanResult = BluetoothLeScanResult.parseFromScanResult(context, result)

            // Check if its a Flipper
            if(BluetoothLeDeviceClassificationHelper.isFlipperDevice(bluetoothLeScanResult)){
                val flipperDeviceScanResult = FlipperDeviceScanResult.parseFromBluetoothLeScanResult(bluetoothLeScanResult)
                addFlipperDeviceToList(flipperDeviceScanResult)
            }

            //Check if its Spam
            if(BluetoothLeDeviceClassificationHelper.isSpamPackage(bluetoothLeScanResult)){
                val spamPackageScanResult = SpamPackageScanResult.parseFromBluetoothLeScanResult(bluetoothLeScanResult)
                addSpamPackageScanResultToList(spamPackageScanResult)
            }

            _bluetoothLeScanServiceCallbacks.forEach { callback ->
                callback.onScanResult(result)
            }
        }
    }

    fun addFlipperDeviceToList(flipperDeviceScanResult: FlipperDeviceScanResult){
        var elementIndex = -1
        _flipperDevicesList.forEach {flipperDevice ->
            if(flipperDevice.address == flipperDeviceScanResult.address){
                elementIndex = _flipperDevicesList.indexOf(flipperDevice)
            }
        }

        val alreadyKnown = elementIndex != -1

        if(elementIndex == -1){
            //  Add
            _flipperDevicesList.add(flipperDeviceScanResult)
        } else {
            // Update
            _flipperDevicesList[elementIndex] = flipperDeviceScanResult
        }

        _bluetoothLeScanServiceCallbacks.forEach {callback ->
            callback.onFlipperListUpdated()
            callback.onFlipperDeviceDetected(flipperDeviceScanResult, alreadyKnown)
        }
    }

    fun addSpamPackageScanResultToList(spamPackageScanResult: SpamPackageScanResult){
        var elementIndex = -1
        _spamPackageScanResultList.forEachIndexed {index, spamPackage ->
            if(spamPackage.address == spamPackageScanResult.address){
                elementIndex = index
            }
        }

        val alreadyKnown = elementIndex != -1

        if(elementIndex == -1){
            //  Add
            _spamPackageScanResultList.add(spamPackageScanResult)
        } else {
            // Update
            _spamPackageScanResultList[elementIndex] = spamPackageScanResult
        }

        _bluetoothLeScanServiceCallbacks.forEach {callback ->
            callback.onSpamResultPackageDetected(spamPackageScanResult, alreadyKnown)
            callback.onSpamResultPackageListUpdated()
        }
    }

    override fun onScanFailed(errorCode: Int) {
        super.onScanFailed(errorCode)

        when(errorCode){
            SCAN_FAILED_ALREADY_STARTED -> Log.e(_logTag, "onScanFailed: SCAN_FAILED_ALREADY_STARTED")
            SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> Log.e(_logTag, "onScanFailed: SCAN_FAILED_APPLICATION_REGISTRATION_FAILED")
            SCAN_FAILED_INTERNAL_ERROR -> Log.e(_logTag, "onScanFailed: SCAN_FAILED_INTERNAL_ERROR")
            SCAN_FAILED_FEATURE_UNSUPPORTED -> Log.e(_logTag, "onScanFailed: SCAN_FAILED_FEATURE_UNSUPPORTED")
            SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> Log.e(_logTag, "onScanFailed: SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES")
            SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> Log.e(_logTag, "onScanFailed: SCAN_FAILED_SCANNING_TOO_FREQUENTLY")
            else -> Log.e(_logTag, "Unknown error for onScanFailed")
        }
    }

}