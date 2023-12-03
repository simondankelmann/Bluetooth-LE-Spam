package de.simon.dankelmann.bluetoothlespam.Models

import java.io.Serializable

class AdvertisementSetList : Serializable {
    var title = ""
    var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

    // Ui Data
    var currentlyAdvertising = false
}