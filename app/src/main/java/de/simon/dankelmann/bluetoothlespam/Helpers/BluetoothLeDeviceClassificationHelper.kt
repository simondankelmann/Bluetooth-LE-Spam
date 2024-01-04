package de.simon.dankelmann.bluetoothlespam.Helpers

import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.LovespousePlayAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.LovespouseStopAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Enums.SpamPackageType
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Models.BluetoothLeScanResult
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import java.util.Locale

class BluetoothLeDeviceClassificationHelper {
    companion object {
        private const val _logTag = "BluetoothLeDeviceClassificationHelper"
        fun isFlipperDevice(bluetoothLeScanResult: BluetoothLeScanResult):Boolean{
            return getFlipperDeviceType(bluetoothLeScanResult) != FlipperDeviceType.UNKNOWN
        }

        fun getFlipperDeviceType(bluetoothLeScanResult: BluetoothLeScanResult):FlipperDeviceType{

            // Flipper UUID's taken from here:
            // https://github.com/K3YOMI/Wall-of-Flippers/blob/main/WallofFlippers.py
            // Credit to "Wall of Flippers" developers
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

        fun isSpamPackage(bluetoothLeScanResult: BluetoothLeScanResult):Boolean{
            return getSpamPackageType(bluetoothLeScanResult) != SpamPackageType.UNKNOWN
        }

        fun getSpamPackageType(bluetoothLeScanResult: BluetoothLeScanResult):SpamPackageType{

            // Fast Pairing
            if(bluetoothLeScanResult.serviceUuids.contains(ParcelUuid.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))){
                return SpamPackageType.FAST_PAIRING
            }

            var spamPackageType = SpamPackageType.UNKNOWN

            //Check if there is manufacturer specific data
            if(bluetoothLeScanResult.manufacturerSpecificData.isNotEmpty()){
                bluetoothLeScanResult.manufacturerSpecificData.forEach { manufacturerId, manufacturerData ->
                    val dataString = manufacturerData.toHexString()

                    // Detect Apple Spam
                    if(manufacturerId == Constants.MANUFACTURER_ID_APPLE){
                        // Check Apple Action Modal, Example: 0f05c027c72b7c OR iOs17 Crash, Example: 0f05c00d39f791000010720dab
                        if(dataString.lowercase(Locale.ROOT).startsWith("0f05c0") || dataString.lowercase(Locale.ROOT).startsWith("0f0540")){
                            if(dataString.contains("000010")){
                               spamPackageType = SpamPackageType.CONTINUITY_IOS_17_CRASH
                            } else {
                                spamPackageType = SpamPackageType.CONTINUITY_ACTION_MODAL
                            }
                        }

                        // NEW AIRTAG, Example: 07190500305546493200006af02720e126b2854197e6e2f07e75d0
                        if(dataString.lowercase(Locale.ROOT).startsWith("071905")){
                            spamPackageType = SpamPackageType.CONTINUITY_NEW_AIRTAG
                        }

                        // NEW DEVICE, Example: 0719070f205560144c00009775af597963b2ac9a29cafcc152f567
                        if(dataString.lowercase(Locale.ROOT).startsWith("071907")){
                            spamPackageType = SpamPackageType.CONTINUITY_NEW_DEVICE
                        }

                        // NOT YOUR DEVICE, Example: 0719010e20550850ca00007f34801a49cb0e3ca35be8ec17222a31
                        if(dataString.lowercase(Locale.ROOT).startsWith("071901")){
                            spamPackageType = SpamPackageType.CONTINUITY_NOT_YOUR_DEVICE
                        }
                    }

                    // Detect Swift Pairing Spam
                    if(manufacturerId == Constants.MANUFACTURER_ID_MICROSOFT){
                        if(dataString.lowercase(Locale.ROOT).startsWith("030080")){
                            spamPackageType = SpamPackageType.SWIFT_PAIRING
                        }
                    }

                    // Detect Easy Setup Spam
                    if(manufacturerId == Constants.MANUFACTURER_ID_SAMSUNG){
                        // Buds, Example:
                        if(dataString.lowercase(Locale.ROOT).startsWith("42098102141503210109".lowercase(Locale.ROOT)) && dataString.lowercase(Locale.ROOT).endsWith("063C948E00000000C700".lowercase(Locale.ROOT))){
                            spamPackageType = SpamPackageType.EASY_SETUP_BUDS
                        }

                        // Watch, Example: 010002000101ff0000431a
                        if(dataString.lowercase(Locale.ROOT).startsWith("010002000101FF000043".lowercase(Locale.ROOT))){
                            spamPackageType = SpamPackageType.EASY_SETUP_WATCH
                        }
                    }

                    // Detect Lovespouse Spam
                    if(manufacturerId == Constants.MANUFACTURER_ID_TYPO_PRODUCTS){
                        Log.d(_logTag, "LS Data: ${dataString}")
                        val prefix = "FFFF006DB643CE97FE427C".lowercase(Locale.ROOT)
                        val prefix2 = "6DB643CE97FE427C".lowercase(Locale.ROOT)
                        val appendix = "03038FAE".lowercase(Locale.ROOT)
                        if((dataString.lowercase(Locale.ROOT).startsWith(prefix) || dataString.lowercase(Locale.ROOT).startsWith(prefix2))){
                            val payload = dataString.replace(appendix, "").replace(prefix, "").replace(prefix2, "")

                            val stopGenerator = LovespouseStopAdvertisementSetGenerator()
                            stopGenerator.lovespouseStops.forEach{
                                if(it.key.lowercase() == payload.lowercase()){
                                    spamPackageType = SpamPackageType.LOVESPOUSE_STOP
                                }
                            }

                            val playGenerator = LovespousePlayAdvertisementSetGenerator()
                            playGenerator.lovespousePlays.forEach{
                                if(it.key.lowercase() == payload.lowercase()){
                                    spamPackageType = SpamPackageType.LOVESPOUSE_PLAY
                                }
                            }
                        }
                    }
                }
            }

            return spamPackageType
        }

    }
}