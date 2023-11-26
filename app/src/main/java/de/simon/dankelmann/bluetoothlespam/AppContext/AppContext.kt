package de.simon.dankelmann.bluetoothlespam.AppContext

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
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

        fun isBluetooth5Supported():Boolean{
            val bluetoothAdapter:BluetoothAdapter? = this.getContext().bluetoothAdapter()
            if(bluetoothAdapter != null) {
                return (bluetoothAdapter.isLe2MPhySupported
                        && bluetoothAdapter.isLeCodedPhySupported
                        && bluetoothAdapter.isLeExtendedAdvertisingSupported
                        && bluetoothAdapter.isLePeriodicAdvertisingSupported)
            }
            return false
        }

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