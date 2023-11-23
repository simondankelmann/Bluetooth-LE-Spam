package de.simon.dankelmann.bluetoothlespam.Models

import java.io.Serializable

class ManufacturerSpecificData : Serializable {
    var id = 0
    var manufacturerId = 0
    var manufacturerSpecificData = byteArrayOf()
}