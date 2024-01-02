package de.simon.dankelmann.bluetoothlespam.Interfaces.Services

import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

interface IAdvertisementService {
    fun startAdvertisement(advertisementSet: AdvertisementSet)
    fun stopAdvertisement()
    fun setTxPowerLevel(txPowerLevel:TxPowerLevel)
    fun getTxPowerLevel(): TxPowerLevel
    fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback)
    fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback)
    fun isLegacyService():Boolean
}