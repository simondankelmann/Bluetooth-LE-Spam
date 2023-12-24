package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.le.AdvertisingSetParameters
import android.os.ParcelUuid
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
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.ServiceData
import java.util.UUID

class FastPairNonProductionAdvertisementSetGenerator:IAdvertisementSetGenerator{

    // Genuine Device Id's taken from here:
    // https://github.com/Flipper-XFW/Xtreme-Firmware/blob/dev/applications/external/ble_spam/protocols/fastpair.c

    val _genuineDeviceIds = mapOf(
        "000007" to "Android Auto",
        "070000" to "Android Auto 2",
        "00000A" to "Anti-Spoof Test",
        "0A0000" to "Anti-Spoof Test 2",
        "000047" to "Arduino 101",
        "470000" to "Arduino 101 2",
        "1E89A7" to "ATS2833_EVB",
        "0001F0" to "Bisto CSR8670 Dev Board",
        "01E5CE" to "BLE-Phone",
        "000048" to "Fast Pair Headphones",
        "480000" to "Fast Pair Headphones 2",
        "000049" to "Fast Pair Headphones 3",
        "490000" to "Fast Pair Headphones 4",
        "000008" to "Foocorp Foophones",
        "080000" to "Foocorp Foophones 2",
        "0200F0" to "Goodyear",
        "F00002" to "Goodyear",
        "00000B" to "Google Gphones",
        "0B0000" to "Google Gphones 2",
        "0C0000" to "Google Gphones 3",
        "001000" to "LG HBS1110",
        "00B727" to "Smart Controller 1",
        "00F7D4" to "Smart Setup",
        "F00400" to "T10",
        "00000D" to "Test 00000D",
        "000035" to "Test 000035",
        "350000" to "Test 000035 2",
        "000009" to "Test Android TV",
        "090000" to "Test Android TV 2"
        )

    val serviceUuid = ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))

    override fun getAdvertisementSets(inputData: Map<String, String>?): List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        val data = inputData ?: _genuineDeviceIds

        data.map {

            var advertisementSet:AdvertisementSet = AdvertisementSet()
            advertisementSet.target = AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID
            advertisementSet.type = AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION
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

            val serviceData = ServiceData()
            serviceData.serviceUuid = serviceUuid
            serviceData.serviceData = StringHelpers.decodeHex(it.key)
            advertisementSet.advertiseData.services.add(serviceData)
            advertisementSet.advertiseData.includeTxPower = true

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