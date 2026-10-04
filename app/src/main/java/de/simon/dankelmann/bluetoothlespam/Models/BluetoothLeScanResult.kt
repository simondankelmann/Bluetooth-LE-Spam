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
    private val _serviceUuids = mutableListOf<ParcelUuid>()
    private val _manufacturerSpecificData = mutableMapOf<Int, ByteArray>()
    
    val serviceUuids: List<ParcelUuid>
        get() = _serviceUuids

    val manufacturerSpecificData: Map<Int, ByteArray>
        get() = _manufacturerSpecificData
    
    fun parseFromBluetoothLeScanResult(bluetoothLeScanResult: BluetoothLeScanResult) {
        deviceName = bluetoothLeScanResult.deviceName
        address = bluetoothLeScanResult.address
        scanRecord = bluetoothLeScanResult.scanRecord
        rssi = bluetoothLeScanResult.rssi
        firstSeen = bluetoothLeScanResult.firstSeen
        lastSeen = bluetoothLeScanResult.lastSeen
        _serviceUuids.clear()
        _serviceUuids.addAll(bluetoothLeScanResult.serviceUuids)
        _manufacturerSpecificData.clear()
        _manufacturerSpecificData.putAll(bluetoothLeScanResult.manufacturerSpecificData)
    }

    companion object {
        private const val _logTag = "BluetoothLeScanResult"

        fun parseFromScanResult(context: Context, scanResult: ScanResult): BluetoothLeScanResult {
            val model = BluetoothLeScanResult()

            scanResult.scanRecord?.let { record ->
                model.scanRecord = record.bytes

                record.serviceUuids?.let { uuids ->
                    model._serviceUuids.addAll(uuids)
                }

                record.manufacturerSpecificData?.let { msd ->
                    msd.forEach { manufacturerId, data ->
                        model._manufacturerSpecificData[manufacturerId] = data
                    }
                }
            }

            model.address = scanResult.device.address

            if (PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, context)) {
                scanResult.device?.name?.let { name ->
                    model.deviceName = name
                }
            }

            model.rssi = scanResult.rssi
            
            return model
        }
    }
}