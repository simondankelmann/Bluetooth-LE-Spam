package de.simon.dankelmann.bluetoothlespam.Helpers

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import de.simon.dankelmann.bluetoothlespam.R


class QueueHandlerHelpers {
    companion object {
        private const val TAG = "QueueHandlerHelpers"

        fun getInterval(context: Context): Long {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val prefKey =
                context.resources.getString(R.string.preference_key_interval_advertising_queue_handler)
            val intervalString = prefs.getString(prefKey, "1000")

            return try {
                val parsedInterval = intervalString?.toLong() ?: 1000L
                if (parsedInterval > 0) {
                    parsedInterval
                } else {
                    1000L
                }
            } catch (e: NumberFormatException) {
                Log.d(TAG, "Invalid interval specified: $intervalString")
                1000L
            }
        }
    }
}
