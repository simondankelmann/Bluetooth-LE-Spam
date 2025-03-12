package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.R


class QueueHandlerHelpers {
    companion object {
        private const val _logTag = "QueueHandlerHelpers"
        private var cachedInterval: Long? = null

        fun getInterval(context: Context): Long {
            cachedInterval?.let { return it }

            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val prefKey = context.resources.getString(R.string.preference_key_interval_advertising_queue_handler)
            val intervalString = preferences.getString(prefKey, "1000")

            return try {
                val parsedInterval = intervalString?.toLong() ?: 1000L
                if (parsedInterval > 0) {
                    cachedInterval = parsedInterval
                    parsedInterval
                } else {
                    1000L
                }
            } catch (e: NumberFormatException) {
                Log.d(_logTag, "Invalid interval specified: $intervalString")
                1000L
            }
        }

        fun clearCache() {
            cachedInterval = null
        }
    }
}