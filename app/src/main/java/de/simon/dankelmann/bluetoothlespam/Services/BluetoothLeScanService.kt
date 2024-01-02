package de.simon.dankelmann.bluetoothlespam.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck

class BluetoothLeScanService () : IBluetoothLeScanService, ScanCallback() {

    private val _logTag = "BluetoothLeScanService"
    private var _bluetoothAdapter:BluetoothAdapter? = null
    private var _bluetoothLeScanner:BluetoothLeScanner? = null
    private var _scanning = false
    private var _bluetoothLeScanServiceCallbacks:MutableList<IBluetoothLeScanCallback> = mutableListOf()


    init {
        _bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
        if(_bluetoothAdapter != null){
            _bluetoothLeScanner = _bluetoothAdapter!!.bluetoothLeScanner
        }
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
                _bluetoothLeScanner!!.startScan(this)
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
        Log.d(_logTag, "onScanResult called")
        if(result != null){
            _bluetoothLeScanServiceCallbacks.forEach { callback ->
                callback.onScanResult(result)
            }

            /*
            val bluetoothDevice = result.device
            val scanRecord = result.scanRecord
            if(scanRecord != null){
                val rawBytes =  result.scanRecord!!.bytes
                Log.d(_logTag, "RAW Result: ${rawBytes.toHexString()}")
            }

            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, AppContext.getActivity())){
                if(bluetoothDevice != null){
                    Log.d(_logTag, "Found Device: ${bluetoothDevice.name} - ${bluetoothDevice.address}")
                }
            }
            */
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