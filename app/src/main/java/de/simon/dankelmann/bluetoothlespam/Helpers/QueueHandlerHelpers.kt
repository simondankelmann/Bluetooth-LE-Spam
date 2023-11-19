package de.simon.dankelmann.bluetoothlespam.Helpers

import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.LegacyAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Services.ModernAdvertisementService
import java.lang.Exception

class QueueHandlerHelpers {
    companion object {
        private const val _logTag = "QueueHandlerHelpers"
        fun getInterval() : Int {
            var interval = 1000

            // Get from Settings, if present
            val preferences = PreferenceManager.getDefaultSharedPreferences(AppContext.getContext()).all
            preferences.forEach {
                if(it.key == AppContext.getActivity().resources.getString(R.string.preference_key_interval_advertising_queue_handler)){
                    val intervalString = it.value as String
                    if(intervalString != null){
                        try {
                            val parsedInterval = intervalString.toInt()
                            if(parsedInterval > 0){
                                interval = parsedInterval
                            }
                        } catch (e: Exception){
                            Log.d(_logTag, "Invalid interval specified: $intervalString")
                        }
                    }
                }
            }

            return interval
        }


    }
}