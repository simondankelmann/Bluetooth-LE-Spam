package de.simon.dankelmann.bluetoothlespam.Models

import android.os.ParcelUuid

class ServiceData {
    var id = 0
    var serviceUuid:ParcelUuid? = null
    var serviceData:ByteArray? = null
}