package de.simon.dankelmann.bluetoothlespam.Helpers

import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.LegacyAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Services.ModernAdvertisementService

class BluetoothHelpers {
    companion object {
        fun supportsBluetooth5():Boolean{
            var bluetoothAdapter = AppContext.getContext().bluetoothAdapter()
            if(bluetoothAdapter != null){
                if(bluetoothAdapter!!.isLe2MPhySupported
                    && bluetoothAdapter!!.isLeCodedPhySupported
                    && bluetoothAdapter!!.isLeExtendedAdvertisingSupported
                    && bluetoothAdapter!!.isLePeriodicAdvertisingSupported
                ){
                    return true
                }
            }
            return false
        }

        fun getAdvertisementService() : IAdvertisementService {

            var useLegacyAdvertisementService = true // <-- DEFAULT

            // Get from Settings, if present
            val preferences = PreferenceManager.getDefaultSharedPreferences(AppContext.getContext()).all
            preferences.forEach {
                if(it.key == AppContext.getActivity().resources.getString(R.string.preference_key_use_legacy_advertising)){
                    useLegacyAdvertisementService = it.value as Boolean
                }
            }

            val advertisementService = when (useLegacyAdvertisementService) {
                true -> LegacyAdvertisementService()
                else -> {
                    ModernAdvertisementService()
                }
            }

            return advertisementService
        }
    }
}