package de.simon.dankelmann.bluetoothlespam.Models

class AdvertisementSetList {
    var title = ""
    var advertisementSets:MutableList<AdvertisementSet> = mutableListOf()

    // Ui Data
    var currentlyAdvertising = false
}