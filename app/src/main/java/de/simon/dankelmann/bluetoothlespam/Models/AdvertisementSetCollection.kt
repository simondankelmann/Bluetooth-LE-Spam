package de.simon.dankelmann.bluetoothlespam.Models

import java.io.Serializable

class AdvertisementSetCollection : Serializable {
    var title = ""
    var advertisementSetLists:MutableList<AdvertisementSetList> = mutableListOf()

    fun getNumberOfLists():Int{
        return advertisementSetLists.count()
    }

    fun getTotalNumberOfAdvertisementSets():Int{
        var totalNumber = 0

        advertisementSetLists.forEach{
            totalNumber += it.advertisementSets.count()
        }

        return totalNumber
    }
}