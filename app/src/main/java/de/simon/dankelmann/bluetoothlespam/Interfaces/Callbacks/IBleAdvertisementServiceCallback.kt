package de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks

import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

interface IBleAdvertisementServiceCallback {
    fun onAdvertisementStarted()
    fun onAdvertisementStopped()
    fun onAdvertisementSetStarted(advertisementSet: AdvertisementSet)
}