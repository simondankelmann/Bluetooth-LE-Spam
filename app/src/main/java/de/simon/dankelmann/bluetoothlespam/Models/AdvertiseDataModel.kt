package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import java.util.UUID

class AdvertiseDataModel {
    private var _logTag = "AdvertiseDataModel"

    var includeDeviceName = true

    /*
    var serviceUuid: ParcelUuid? = null
    var serviceData = StringHelpers.decodeHex("00")*/

    var services = mutableListOf<ServiceDataModel>()
    var includeTxPower = true
    var manufacturerData = mutableListOf<ManufacturerSpecificDataModel>()

    fun validate():Boolean{
        //@Todo: implement validation here
        return true
    }
    fun build() : AdvertiseData?{
        if(validate()){
            var builder = AdvertiseData.Builder()

            builder.setIncludeDeviceName(includeDeviceName)

            services.forEach {
                if(it.serviceUuid != null){
                    builder.addServiceUuid(it.serviceUuid)
                    if(it.serviceData != null){
                        builder.addServiceData(it.serviceUuid, it.serviceData)
                    }
                }
            }

            builder.setIncludeTxPowerLevel(includeTxPower)

            manufacturerData.forEach {
                builder.addManufacturerData(it.manufacturerId, it.manufacturerSpecificData)
            }

            return builder.build()
        } else {
            Log.d(_logTag, "AdvertiseDataModel could not be built because its invalid")
        }
        return null
    }
}