package de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks

import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

interface IAdvertisementServiceCallback {
    fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?)
    fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError)
}