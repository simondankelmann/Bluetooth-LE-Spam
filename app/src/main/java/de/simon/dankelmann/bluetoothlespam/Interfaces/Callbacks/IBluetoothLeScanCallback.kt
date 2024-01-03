package de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks

import android.bluetooth.le.ScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult

interface IBluetoothLeScanCallback {
    fun onScanResult(scanResult:ScanResult)
    fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown:Boolean)
}