package de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks

import android.bluetooth.le.ScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult

interface IBluetoothLeScanCallback {
    fun onScanResult(scanResult:ScanResult)
    fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown:Boolean)
    fun onFlipperListUpdated()
    fun onSpamResultPackageDetected(spamPackageScanResult: SpamPackageScanResult, alreadyKnown:Boolean)
    fun onSpamResultPackageListUpdated()


}