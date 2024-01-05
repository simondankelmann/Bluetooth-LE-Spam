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

            spamPackageScanResult.parseFromBluetoothLeScanResult(bluetoothLeScanResult)
            spamPackageScanResult.spamPackageType = BluetoothLeDeviceClassificationHelper.getSpamPackageType(bluetoothLeScanResult)

            return spamPackageScanResult
        }
    }
}