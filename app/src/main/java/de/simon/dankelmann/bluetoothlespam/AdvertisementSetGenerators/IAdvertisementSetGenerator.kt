package de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators

import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

interface IAdvertisementSetGenerator {
    fun getAdvertisementSets(inputData: Map<String, String>?):List<AdvertisementSet>

}