package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import java.util.UUID

class AdvertiseDataModel {
    private var _logTag = "AdvertiseDataModel"

    var includeDeviceName = true
    var serviceUuid = ParcelUuid(UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb"))
    var serviceData = StringHelpers.decodeHex("00")
    var includeTxPower = true

    fun validate():Boolean{
        //@Todo: implement validation here
        return true
    }
    fun build() : AdvertiseData?{
        if(validate()){
            return AdvertiseData.Builder()
                .setIncludeDeviceName(includeDeviceName)
                .addServiceUuid(serviceUuid)
                .addServiceData(serviceUuid, serviceData)
                //.addManufacturerData(manufacturerId, serviceData)
                //.addManufacturerData(manufacturerId, manufacturerSpecificData)
                .setIncludeTxPowerLevel(includeTxPower)
                .build()
        } else {
            Log.d(_logTag, "AdvertiseDataModel could not be built because its invalid")
        }
        return null
    }
}