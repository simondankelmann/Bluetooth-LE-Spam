package de.simon.dankelmann.bluetoothlespam.Models

import java.io.Serializable

class AdvertisementSetCollection : Serializable {
    var title = ""
    var advertisementSetLists:MutableList<AdvertisementSetList> = mutableListOf()
}