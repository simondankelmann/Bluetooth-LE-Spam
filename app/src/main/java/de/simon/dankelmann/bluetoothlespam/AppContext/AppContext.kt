package de.simon.dankelmann.bluetoothlespam.AppContext

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context

abstract class AppContext {
    companion object {

        private lateinit var context: Context
        private lateinit var activity: Activity

        fun Context.bluetoothAdapter(): BluetoothAdapter? =
            (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        fun Context.bluetoothManager(): BluetoothManager? =
            (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)


        fun setContext(con: Context) {
            context=con
        }

        fun getContext(): Context {
            return context
        }

        fun setActivity(act: Activity) {
            activity=act
        }

        fun getActivity(): Activity {
            return activity
        }

        fun registerPermissionCallback(requestCode: Int, callback:Runnable){

        }

    }
}