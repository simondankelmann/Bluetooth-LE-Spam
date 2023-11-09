package de.simon.dankelmann.bluetoothlespam.AppContext

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService

abstract class AppContext {
    companion object {

        private lateinit var _context: Context
        private lateinit var _activity: Activity
        private lateinit var _advertisementService: IAdvertisementService
        private lateinit var _advertisementSetQueueHandler: AdvertisementSetQueueHandler

        fun Context.bluetoothAdapter(): BluetoothAdapter? = (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        fun Context.bluetoothManager(): BluetoothManager? = (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)


        fun setContext(context: Context) {
            _context = context
        }

        fun getContext(): Context {
            return _context
        }

        fun setActivity(activity: Activity) {
            _activity = activity
        }

        fun getActivity(): Activity {
            return _activity
        }

        fun setAdvertisementService(advertisementService: IAdvertisementService) {
            _advertisementService = advertisementService
        }

        fun getAdvertisementService(): IAdvertisementService {
            return _advertisementService
        }

        fun setAdvertisementSetQueueHandler(advertisementSetQueueHandler: AdvertisementSetQueueHandler) {
            _advertisementSetQueueHandler = advertisementSetQueueHandler
        }

        fun getAdvertisementSetQueueHandler(): AdvertisementSetQueueHandler {
            return _advertisementSetQueueHandler
        }

        fun registerPermissionCallback(requestCode: Int, callback:Runnable){

        }

    }
}