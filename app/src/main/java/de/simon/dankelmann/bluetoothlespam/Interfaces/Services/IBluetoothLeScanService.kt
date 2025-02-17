package de.simon.dankelmann.bluetoothlespam.Interfaces.Services

import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult

interface IBluetoothLeScanService {
    fun startScanning()
    fun stopScanning()

    fun isScanning(): Boolean

    fun getFlipperDevicesList(): MutableList<FlipperDeviceScanResult>
    fun getSpamPackageScanResultList(): MutableList<SpamPackageScanResult>

    fun addBluetoothLeScanServiceCallback(callback: IBluetoothLeScanCallback)
    fun removeBluetoothLeScanServiceCallback(callback: IBluetoothLeScanCallback)
}
