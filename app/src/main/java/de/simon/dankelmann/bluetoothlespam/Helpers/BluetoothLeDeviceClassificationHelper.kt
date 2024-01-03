package de.simon.dankelmann.bluetoothlespam.Helpers

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult

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

        fun getFlipperDeviceSanResult(scanResult: ScanResult):FlipperDeviceScanResult{
            var flipperDeviceScanResult:FlipperDeviceScanResult = BluetoothLeScanResult.parseFromScanResult(scanResult) as FlipperDeviceScanResult

            flipperDeviceScanResult.flipperDeviceType = getFlipperDeviceType(flipperDeviceScanResult)
            return flipperDeviceScanResult
        }
    }
}