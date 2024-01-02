package de.simon.dankelmann.bluetoothlespam.Interfaces.Services

import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback

interface IBluetoothLeScanService {
    fun startScanning()

    fun stopScanning()

    fun addBluetoothLeScanServiceCallback(callback: IBluetoothLeScanCallback)
    fun removeBluetoothLeScanServiceCallback(callback: IBluetoothLeScanCallback)
}