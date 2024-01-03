package de.simon.dankelmann.bluetoothlespam.Interfaces.Services

import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult

interface IBluetoothLeScanService {
    fun startScanning()

    fun stopScanning()

    fun getFlipperDevicesList():List<FlipperDeviceScanResult>

    fun addBluetoothLeScanServiceCallback(callback: IBluetoothLeScanCallback)
    fun removeBluetoothLeScanServiceCallback(callback: IBluetoothLeScanCallback)
}