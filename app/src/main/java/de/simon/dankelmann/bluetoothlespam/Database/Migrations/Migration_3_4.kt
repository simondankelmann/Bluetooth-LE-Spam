package de.simon.dankelmann.bluetoothlespam.Database.Migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds manually-curated Quick Start membership: `isQuickStart` (checked in the new "Manage Quick
 * Start" screen) and `quickStartOrder` (position among checked collections, set to the next
 * highest value when checked, cleared when unchecked -- not recency-based).
 */
val Migration_3_4 = object : Migration(3, 4) {
    private val _logTag = "Migration_3_4"

    override fun migrate(db: SupportSQLiteDatabase) {
        Log.d(_logTag, "Executing Migration...")

        db.execSQL("ALTER TABLE `AdvertisementSetCollectionEntity` ADD COLUMN `isQuickStart` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `AdvertisementSetCollectionEntity` ADD COLUMN `quickStartOrder` INTEGER")
    }
}
