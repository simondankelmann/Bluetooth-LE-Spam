package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.R


class QueueHandlerHelpers {
    companion object {
        private const val _logTag = "QueueHandlerHelpers"

        fun getInterval(context: Context): Long {
            // Get from Settings, if present
            val preferences = PreferenceManager.getDefaultSharedPreferences(context).all
            val prefKey =
                context.resources.getString(R.string.preference_key_interval_advertising_queue_handler)
            preferences.forEach {
                if (it.key == prefKey) {
                    val intervalString = it.value as? String
                    if (intervalString != null) {
                        try {
                            val parsedInterval = intervalString.toLong()
                            if (parsedInterval > 0) {
                                return parsedInterval
                            }
                        } catch (e: NumberFormatException) {
                            Log.d(_logTag, "Invalid interval specified: $intervalString")
                        }
                    }
                }
            }
            return 1000
        }
    }
}