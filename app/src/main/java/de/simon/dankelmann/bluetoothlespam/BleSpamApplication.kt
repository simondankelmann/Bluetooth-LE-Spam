package de.simon.dankelmann.bluetoothlespam

import android.app.Application
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanService

class BleSpamApplication : Application() {

    lateinit var scanService: IBluetoothLeScanService
        private set

    override fun onCreate() {
        super.onCreate()

        scanService = BluetoothLeScanService(this)
    }
}
