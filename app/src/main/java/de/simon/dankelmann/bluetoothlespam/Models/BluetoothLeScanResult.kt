package de.simon.dankelmann.bluetoothlespam.Models

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.ParcelUuid
import androidx.core.util.forEach
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import java.time.LocalDateTime

open class BluetoothLeScanResult {
    private val logTag = "BluetoothLeScanResult"
    var deviceName = ""
    var address = ""
    var scanRecord = byteArrayOf()
    var rssi = 0
    var firstSeen = LocalDateTime.now()
    var lastSeen = LocalDateTime.now()
    var serviceUuids = mutableListOf<ParcelUuid>()
    var manufacturerSpecificData = mutableMapOf<Int, ByteArray>()
    
    fun parseFromBluetoothLeScanResult(bluetoothLeScanResult: BluetoothLeScanResult){
        deviceName = bluetoothLeScanResult.deviceName
        address = bluetoothLeScanResult.address
        scanRecord = bluetoothLeScanResult.scanRecord
        rssi = bluetoothLeScanResult.rssi
        firstSeen = bluetoothLeScanResult.firstSeen
        lastSeen = bluetoothLeScanResult.lastSeen
        serviceUuids = bluetoothLeScanResult.serviceUuids
        manufacturerSpecificData = bluetoothLeScanResult.manufacturerSpecificData
    }

    companion object {

        private const val _logTag = "BluetoothLeScanResult"

        fun parseFromScanResult(context: Context, scanResult: ScanResult): BluetoothLeScanResult {
            var model = BluetoothLeScanResult()

            // get raw message
            if(scanResult.scanRecord != null){
                model.scanRecord = scanResult.scanRecord!!.bytes

                var serviceUuids = scanResult.scanRecord!!.serviceUuids
                if(serviceUuids != null){
                    serviceUuids.forEach{
                        model.serviceUuids.add(it)
                    }
                }

                // get manufacturer specific data
                if(scanResult.scanRecord!!.manufacturerSpecificData != null){
                    val resultManufacturerSpecificData = scanResult.scanRecord!!.manufacturerSpecificData
                    resultManufacturerSpecificData.forEach{manufacurerId, data ->
                        //Log.d(_logTag, "ID: ${manufacurerId} Data: ${data.toHexString()}")
                        model.manufacturerSpecificData[manufacurerId] = data
                    }
                }
            }

            // get mac address
            model.address = scanResult.device.address

            // get device data
            if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, context)) {
                if (scanResult.device != null && scanResult.device.name != null) {
                    model.deviceName = scanResult.device.name
                }
            }

            // get rssi
            model.rssi = scanResult.rssi
            
            return model
        }
    }
}