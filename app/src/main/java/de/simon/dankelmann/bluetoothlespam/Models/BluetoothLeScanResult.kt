package de.simon.dankelmann.bluetoothlespam.Models

import android.Manifest
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import java.sql.Time
import java.time.LocalDate

open class BluetoothLeScanResult {
    var deviceName = ""
    var address = ""
    var scanRecord = byteArrayOf()
    var rssi = 0
    var firstSeen = LocalDate.now()
    var lastSeen = LocalDate.now()
    var serviceUuids = mutableListOf<ParcelUuid>()

    companion object {
        fun parseFromScanResult(scanResult: ScanResult):BluetoothLeScanResult{
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
            }

            model.address = scanResult.device.address

            // get device data
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, AppContext.getActivity())){
                if(scanResult.device != null && scanResult.device.name != null){
                    model.deviceName = scanResult.device.name
                }
            }

            model.rssi = scanResult.rssi

            return model
        }
    }
}