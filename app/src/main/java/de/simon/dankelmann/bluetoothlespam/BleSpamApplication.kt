package de.simon.dankelmann.bluetoothlespam

import android.app.Application
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.ThemeManager
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanService


class BleSpamApplication : Application() {

    lateinit var advertisementService: IAdvertisementService
        private set

    lateinit var queueHandler: AdvertisementSetQueueHandler
        private set

    lateinit var scanService: IBluetoothLeScanService
        private set

    override fun onCreate() {
        // One-time blocking load of the DataStore-backed settings (theme mode/seed color,
        // dynamic color, blur) so every synchronous ThemeManager read below and throughout the
        // app sees real (migrated) values instead of defaults — must happen before ANY read.
        SettingsRepository.getInstance(this).warmUp()

        // Apply the user's theme preference before calling super.onCreate()
        // to ensure the theme is set before any UI is created
        ThemeManager.getInstance().applyTheme(this)

        super.onCreate()

        // Dynamic color for the classic-View screens is applied per-Activity in
        // MainActivity.onCreate() instead of app-wide here — it needs to react to the
        // theme-picker's custom seed color (DynamicColorsOptions.setContentBasedSource),
        // not just the device wallpaper, and MainActivity is this app's only Activity.

        setupAdvertisementService()
        scanService = BluetoothLeScanService(this)
    }

    fun setupAdvertisementService() {
        advertisementService = BluetoothHelpers.getAdvertisementService(this)
        queueHandler = AdvertisementSetQueueHandler(this, advertisementService)
    }
}
