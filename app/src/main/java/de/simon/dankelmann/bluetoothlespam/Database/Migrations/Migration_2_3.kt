package de.simon.dankelmann.bluetoothlespam.Database.Migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers

/**
 * Activates the dormant AdvertisementSetList/AdvertisementSetCollection schema (plan §8):
 * - `lastUsedAt`/`isCustom` columns for Quick Start + built-in-vs-custom distinction.
 * - Real foreign keys on both join tables (list<->set, collection<->list) — SQLite can't
 *   `ALTER TABLE ADD FOREIGN KEY`, so those two tables are recreated. Both were 100% empty
 *   before this migration (confirmed: no code anywhere ever wrote to them), so the recreation
 *   has no data to preserve for THOSE two tables specifically — the REGRESSION-critical data is
 *   the existing `AdvertisementSetEntity` rows, which this migration only reads (via
 *   [DatabaseHelpers.seedBuiltInListsAndCollections]), never touches destructively.
 *
 * Delete-cascade policy (plan §8): a collection's own join rows are removed when the collection
 * is deleted (CASCADE); a list is never implicitly removed just because a collection referencing
 * it was deleted (NO_ACTION) — lists can be shared across collections (e.g. Kitchen Sink reuses
 * every other built-in collection's lists). List<->set is CASCADE both ways.
 */
val Migration_2_3 = object : Migration(2, 3) {
    private val _logTag = "Migration_2_3"

    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d(_logTag, "Executing Migration...")

        database.execSQL("ALTER TABLE `AdvertisementSetCollectionEntity` ADD COLUMN `lastUsedAt` INTEGER")
        database.execSQL("ALTER TABLE `AdvertisementSetCollectionEntity` ADD COLUMN `isCustom` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `AdvertisementSetListEntity` ADD COLUMN `lastUsedAt` INTEGER")

        // AssociatonCollectionListEntity: recreate with FKs (collection CASCADE, list NO_ACTION).
        database.execSQL(
            "CREATE TABLE `AssociatonCollectionListEntity_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`advertisementSetCollectionId` INTEGER NOT NULL, " +
                "`advertisementSetListId` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`advertisementSetCollectionId`) REFERENCES `AdvertisementSetCollectionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`advertisementSetListId`) REFERENCES `AdvertisementSetListEntity`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)",
        )
        database.execSQL(
            "INSERT INTO `AssociatonCollectionListEntity_new` (id, advertisementSetCollectionId, advertisementSetListId, position) " +
                "SELECT id, advertisementSetCollectionId, advertisementSetListId, position FROM `AssociatonCollectionListEntity`",
        )
        database.execSQL("DROP TABLE `AssociatonCollectionListEntity`")
        database.execSQL("ALTER TABLE `AssociatonCollectionListEntity_new` RENAME TO `AssociatonCollectionListEntity`")
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_AssociatonCollectionListEntity_advertisementSetCollectionId` " +
                "ON `AssociatonCollectionListEntity` (`advertisementSetCollectionId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_AssociatonCollectionListEntity_advertisementSetListId` " +
                "ON `AssociatonCollectionListEntity` (`advertisementSetListId`)",
        )

        // AssociationListSetEntity: recreate with FKs (both sides CASCADE).
        database.execSQL(
            "CREATE TABLE `AssociationListSetEntity_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`advertisementSetId` INTEGER NOT NULL, " +
                "`advertisementSetListId` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`advertisementSetListId`) REFERENCES `AdvertisementSetListEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`advertisementSetId`) REFERENCES `AdvertisementSetEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
        database.execSQL(
            "INSERT INTO `AssociationListSetEntity_new` (id, advertisementSetId, advertisementSetListId, position) " +
                "SELECT id, advertisementSetId, advertisementSetListId, position FROM `AssociationListSetEntity`",
        )
        database.execSQL("DROP TABLE `AssociationListSetEntity`")
        database.execSQL("ALTER TABLE `AssociationListSetEntity_new` RENAME TO `AssociationListSetEntity`")
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_AssociationListSetEntity_advertisementSetListId` " +
                "ON `AssociationListSetEntity` (`advertisementSetListId`)",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_AssociationListSetEntity_advertisementSetId` " +
                "ON `AssociationListSetEntity` (`advertisementSetId`)",
        )

        // Seed the built-in lists/collections from the AdvertisementSet rows that already exist
        // (same fire-and-forget-Thread pattern as Migration_1_2 — the rest of the app already
        // tolerates isSeeding being true for a beat after a migration/create).
        Thread {
            synchronized(this) {
                AppDatabase.getInstance().isSeeding = true
                DatabaseHelpers.seedBuiltInListsAndCollections()
                AppDatabase.getInstance().isSeeding = false
            }
        }.start()

        Log.d(_logTag, "Finished Executing Migration...")
    }
}
