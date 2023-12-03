package de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks

import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

interface IAdvertisementSetQueueHandlerCallback {
    fun onQueueHandlerActivated()
    fun onQueueHandlerDeactivated()
}