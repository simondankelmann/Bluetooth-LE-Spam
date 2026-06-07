package de.simon.dankelmann.bluetoothlespam

import android.app.Application
import android.os.Build
import com.google.android.material.color.DynamicColors
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
        // 应用 Material 3 Dynamic Colors (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
        // 应用主题
        ThemeManager.getInstance().applyTheme(this)

        super.onCreate()

        setupAdvertisementService()
        scanService = BluetoothLeScanService(this)
    }

    fun setupAdvertisementService() {
        advertisementService = BluetoothHelpers.getAdvertisementService(this)
        queueHandler = AdvertisementSetQueueHandler(this, advertisementService)
    }
}
