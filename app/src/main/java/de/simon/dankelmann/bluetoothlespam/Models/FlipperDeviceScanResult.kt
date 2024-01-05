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

            flipperDeviceScanResult.parseFromBluetoothLeScanResult(bluetoothLeScanResult)
            flipperDeviceScanResult.flipperDeviceType = BluetoothLeDeviceClassificationHelper.getFlipperDeviceType(bluetoothLeScanResult)

            return flipperDeviceScanResult
        }
    }
}