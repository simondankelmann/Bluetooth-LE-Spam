package de.simon.dankelmann.bluetoothlespam.Models

import android.os.ParcelUuid
import java.io.Serializable

class ServiceData : Serializable {
    var id = 0
    var serviceUuid:ParcelUuid? = null
    var serviceData:ByteArray? = null
}