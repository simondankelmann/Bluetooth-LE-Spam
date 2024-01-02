package de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks

import android.bluetooth.le.ScanResult

interface IBluetoothLeScanCallback {
    fun onScanResult(scanResult:ScanResult)

}