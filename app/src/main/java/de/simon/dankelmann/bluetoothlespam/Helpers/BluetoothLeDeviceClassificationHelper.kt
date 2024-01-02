package de.simon.dankelmann.bluetoothlespam.Helpers

import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult

class BluetoothLeDeviceClassificationHelper {
    companion object {
        fun isFlipperDevice(bluetoothLeScanResult: BluetoothLeScanResult):Boolean{
            return getFlipperDeviceType(bluetoothLeScanResult) != FlipperDeviceType.UNKNOWN
        }

        fun getFlipperDeviceType(bluetoothLeScanResult: BluetoothLeScanResult):FlipperDeviceType{

            if(bluetoothLeScanResult.serviceUuids.isNotEmpty()){
                if(bluetoothLeScanResult.serviceUuids.contains(ParcelUuid.fromString("00003082-0000-1000-8000-00805f9b34fb"))){
                    return FlipperDeviceType.FLIPPER_ZERO_WHITE
                }

                if(bluetoothLeScanResult.serviceUuids.contains(ParcelUuid.fromString("00003081-0000-1000-8000-00805f9b34fb"))){
                    return FlipperDeviceType.FLIPPER_ZERO_BLACK
                }

                if(bluetoothLeScanResult.serviceUuids.contains(ParcelUuid.fromString("00003083-0000-1000-8000-00805f9b34fb"))){
                    return FlipperDeviceType.FLIPPER_ZERO_TRANSPARENT
                }
            }

            return FlipperDeviceType.UNKNOWN
        }
    }
}