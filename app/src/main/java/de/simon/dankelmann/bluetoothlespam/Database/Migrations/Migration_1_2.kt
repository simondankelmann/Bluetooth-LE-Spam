package de.simon.dankelmann.bluetoothlespam.Database.Migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityActionModalAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers

val Migration_1_2 = object : Migration(1,2) {
    private val _logTag = "Migration_1_2"


    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(_logTag, "Executing Migration...")

        val nearbyActionsAdded = mapOf(
            "05" to "Apple Watch",
            "24" to "Apple Vision Pro",
            "2F" to "Connect to other Device",
            "21" to "Software Update",
        )


        val continuityActionModalAdvertisementSetGenerator = ContinuityActionModalAdvertisementSetGenerator()
        val nearbyActionsAddedAdvertisementSets = continuityActionModalAdvertisementSetGenerator.getAdvertisementSets(nearbyActionsAdded)

        Thread {
            synchronized(this) {
                AppDatabase.getInstance().isSeeding = true
                nearbyActionsAddedAdvertisementSets.forEach{ advertisementSet ->
                    DatabaseHelpers.saveAdvertisementSet(advertisementSet)
                }
                AppDatabase.getInstance().isSeeding = false
            }
        }.start()

        Log.d(_logTag, "Finished Executing Migration...")
    }
}