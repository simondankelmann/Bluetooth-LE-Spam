package de.simon.dankelmann.bluetoothlespam.Models

import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Enums.SpamPackageType
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothLeDeviceClassificationHelper
import java.time.LocalDate

class SpamPackageScanResult: BluetoothLeScanResult() {
    var spamPackageType = SpamPackageType.UNKNOWN

    companion object {
        fun parseFromBluetoothLeScanResult(bluetoothLeScanResult: BluetoothLeScanResult):SpamPackageScanResult{
            val spamPackageScanResult = SpamPackageScanResult()

            spamPackageScanResult.deviceName = bluetoothLeScanResult.deviceName
            spamPackageScanResult.address = bluetoothLeScanResult.address
            spamPackageScanResult.scanRecord = bluetoothLeScanResult.scanRecord
            spamPackageScanResult.rssi = bluetoothLeScanResult.rssi
            spamPackageScanResult.firstSeen = bluetoothLeScanResult.firstSeen
            spamPackageScanResult.lastSeen = bluetoothLeScanResult.lastSeen
            spamPackageScanResult.serviceUuids = bluetoothLeScanResult.serviceUuids

            spamPackageScanResult.spamPackageType = BluetoothLeDeviceClassificationHelper.getSpamPackageType(bluetoothLeScanResult)

            return spamPackageScanResult
        }
    }
}