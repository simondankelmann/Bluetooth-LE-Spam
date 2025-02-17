package de.simon.dankelmann.bluetoothlespam.AppContext

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService

abstract class AppContext {
    companion object {

        private lateinit var _context: Context
        private lateinit var _advertisementService: IAdvertisementService
        private lateinit var _advertisementSetQueueHandler: AdvertisementSetQueueHandler

        fun setContext(context: Context) {
            _context = context
        }

        fun getContext(): Context {
            return _context
        }

        fun setAdvertisementService(advertisementService: IAdvertisementService) {
            _advertisementService = advertisementService
        }

        fun getAdvertisementService(): IAdvertisementService {
            return _advertisementService
        }

        fun advertisementServiceIsInitialized(): Boolean {
            return this::_advertisementService.isInitialized
        }

        fun setAdvertisementSetQueueHandler(advertisementSetQueueHandler: AdvertisementSetQueueHandler) {
            _advertisementSetQueueHandler = advertisementSetQueueHandler
        }

        fun getAdvertisementSetQueueHandler(): AdvertisementSetQueueHandler {
            return _advertisementSetQueueHandler
        }

        fun advertisementSetQueueHandlerIsInitialized(): Boolean {
            return this::_advertisementSetQueueHandler.isInitialized
        }

        fun registerPermissionCallback(requestCode: Int, callback:Runnable){

        }

    }
}