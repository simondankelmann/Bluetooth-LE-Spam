package de.simon.dankelmann.bluetoothlespam.Helpers

import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter

class BluetoothHelpers {
    companion object {
        fun supportsBluetooth5():Boolean{
            var bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
            if(bluetoothAdapter != null){
                if(bluetoothAdapter!!.isLe2MPhySupported
                    && bluetoothAdapter!!.isLeCodedPhySupported
                    && bluetoothAdapter!!.isLeExtendedAdvertisingSupported
                    && bluetoothAdapter!!.isLePeriodicAdvertisingSupported
                ){
                    return true
                }
            }
            return false
        }
    }
}