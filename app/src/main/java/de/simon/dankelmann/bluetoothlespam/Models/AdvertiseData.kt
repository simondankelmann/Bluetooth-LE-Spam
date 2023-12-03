package de.simon.dankelmann.bluetoothlespam.Models

import android.bluetooth.le.AdvertiseData
import android.util.Log
import java.io.Serializable

class AdvertiseData : Serializable {
    private var _logTag = "AdvertiseData"

    var id = 0
    var includeDeviceName = true
    var includeTxPower = true

    var manufacturerData = mutableListOf<ManufacturerSpecificData>()
    var services = mutableListOf<ServiceData>()


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