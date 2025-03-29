package de.simon.dankelmann.bluetoothlespam

import android.app.Application
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanService

class BleSpamApplication : Application() {

    lateinit var scanService: IBluetoothLeScanService
        private set

    override fun onCreate() {
        // Apply the user's theme preference before calling super.onCreate()
        // to ensure the theme is set before any UI is created
        ThemeManager.getInstance().applyTheme(this)

        super.onCreate()

        scanService = BluetoothLeScanService(this)
    }
}
