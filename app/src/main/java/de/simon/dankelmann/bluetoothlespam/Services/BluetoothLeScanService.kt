package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.room.util.recursiveFetchArrayMap
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothLeDeviceClassificationHelper
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck

class BluetoothLeScanService () : IBluetoothLeScanService, ScanCallback() {

    private val _logTag = "BluetoothLeScanService"
    private var _bluetoothAdapter:BluetoothAdapter? = null
    private var _bluetoothLeScanner:BluetoothLeScanner? = null
    private var _scanning = false
    private var _bluetoothLeScanServiceCallbacks:MutableList<IBluetoothLeScanCallback> = mutableListOf()

    private var _flipperDevicesList = mutableListOf<FlipperDeviceScanResult>()


    init {
        _bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
        if(_bluetoothAdapter != null){
            _bluetoothLeScanner = _bluetoothAdapter!!.bluetoothLeScanner
        }
    }

    override fun getFlipperDevicesList():List<FlipperDeviceScanResult>{
        return _flipperDevicesList.toList()
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

    override fun startScanning(){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_SCAN, AppContext.getActivity())){
            if(_bluetoothLeScanner != null){
                // SET THE FILTERS AND SETTINGS
                val filterList:List<ScanFilter> = listOf(ScanFilter.Builder().build())
                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

                _bluetoothLeScanner!!.startScan(filterList, settings, this)
                Log.d(_logTag, "Started BLE Scan")
                _scanning = true
            }
        }
    }

    override fun stopScanning(){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_SCAN, AppContext.getActivity())){
            if(_bluetoothLeScanner != null) {
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
            val bluetoothLeScanResult = BluetoothLeScanResult.parseFromScanResult(result)

            if(BluetoothLeDeviceClassificationHelper.isFlipperDevice(bluetoothLeScanResult)){
                val flipperDeviceScanResult = FlipperDeviceScanResult.parseFromBluetoothLeScanResult(bluetoothLeScanResult)
                addFlipperDeviceToList(flipperDeviceScanResult)
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
            callback.onFlipperDeviceDetected(flipperDeviceScanResult, alreadyKnown)
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