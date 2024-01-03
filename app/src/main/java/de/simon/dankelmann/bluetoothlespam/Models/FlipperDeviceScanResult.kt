package de.simon.dankelmann.bluetoothlespam.Models

import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothLeDeviceClassificationHelper
import java.time.LocalDate

class FlipperDeviceScanResult: BluetoothLeScanResult() {
    var flipperDeviceType = FlipperDeviceType.UNKNOWN
    var isSpamming = false
    companion object {
        fun parseFromBluetoothLeScanResult(bluetoothLeScanResult: BluetoothLeScanResult):FlipperDeviceScanResult{
            val flipperDeviceScanResult = FlipperDeviceScanResult()

            flipperDeviceScanResult.deviceName = bluetoothLeScanResult.deviceName
            flipperDeviceScanResult.address = bluetoothLeScanResult.address
            flipperDeviceScanResult.scanRecord = bluetoothLeScanResult.scanRecord
            flipperDeviceScanResult.rssi = bluetoothLeScanResult.rssi
            flipperDeviceScanResult.firstSeen = bluetoothLeScanResult.firstSeen
            flipperDeviceScanResult.lastSeen = bluetoothLeScanResult.lastSeen
            flipperDeviceScanResult.serviceUuids = bluetoothLeScanResult.serviceUuids

            flipperDeviceScanResult.flipperDeviceType = BluetoothLeDeviceClassificationHelper.getFlipperDeviceType(bluetoothLeScanResult)

            return flipperDeviceScanResult
        }
    }
}