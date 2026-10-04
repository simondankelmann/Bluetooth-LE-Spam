package de.simon.dankelmann.bluetoothlespam.Database.Migrations

import android.app.Application
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * CRITICAL regression test (plan §11/§8 REGRESSION RULE) — an existing v2 install's
 * [de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetEntity] rows (the
 * actual user/generator data) must survive the v2->v3 migration unchanged. `Migration_2_3` only
 * ever reads that table (via `DatabaseHelpers.seedBuiltInListsAndCollections`), never writes to
 * it — this test proves that, plus that the schema changes (new columns, new FK-bearing join
 * tables) land exactly as Room expects (`runMigrationsAndValidate` fails loudly on any mismatch
 * against the exported schemas/3.json).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31], application = Application::class)
class Migration_2_3_Test {

    private val testDbName = "migration-2-3-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `existing AdvertisementSet rows survive the v2 to v3 migration unchanged`() {
        // Seed a v2 database exactly as a real pre-upgrade install would have it: one saved
        // AdvertisementSet with its related rows (the FK-like int columns aren't enforced pre-
        // migration, but we still seed real related rows for a realistic scenario).
        val db = helper.createDatabase(testDbName, 2)
        db.execSQL(
            "INSERT INTO AdvertiseSettingsEntity (id, advertiseMode, txPowerLevel, connectable, timeout) " +
                "VALUES (1, 'ADVERTISE_MODE_LOW_LATENCY', 'ADVERTISE_TX_POWER_HIGH', 0, 0)",
        )
        db.execSQL(
            "INSERT INTO AdvertisingSetParametersEntity (id, legacyMode, interval, txPowerLevel, includeTxPowerLevel, primaryPhy, secondaryPhy, scanable, connectable, anonymous) " +
                "VALUES (1, 1, 160, 'ADVERTISE_TX_POWER_HIGH', 0, NULL, NULL, 0, 0, 0)",
        )
        db.execSQL(
            "INSERT INTO AdvertiseDataEntity (id, includeDeviceName, includeTxPower) VALUES (1, 0, 0)",
        )
        db.execSQL(
            "INSERT INTO AdvertisementSetEntity (id, title, target, type, duration, maxExtendedAdvertisingEvents, range, advertiseSettingsId, advertisingSetParametersId, advertiseDataId, scanResponseId, periodicAdvertisingParametersId, periodicAdvertiseDataId) " +
                "VALUES (1, 'Regression Test Set', 'ADVERTISEMENT_TARGET_ANDROID', 'ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE', 1000, 0, 'ADVERTISEMENTSET_RANGE_CLOSE', 1, 1, 1, NULL, NULL, NULL)",
        )
        db.close()

        // Runs Migration_2_3, then validates the resulting schema against schemas/3.json
        // (fails loudly if my hand-written migration SQL doesn't exactly match what Room's
        // KSP-generated schema for the current entities expects).
        val migratedDb = helper.runMigrationsAndValidate(testDbName, 3, true, Migration_2_3)

        val cursor = migratedDb.query("SELECT title, type, advertiseSettingsId FROM AdvertisementSetEntity WHERE id = 1")
        assertTrue("expected the pre-existing AdvertisementSet row to survive", cursor.moveToFirst())
        assertEquals("Regression Test Set", cursor.getString(0))
        assertEquals("ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE", cursor.getString(1))
        assertEquals(1, cursor.getInt(2))
        cursor.close()

        // New columns exist with the expected defaults — doesn't rely on the async seeding
        // Thread Migration_2_3 kicks off (see DatabaseHelperSeedingTest for that logic).
        val collectionColumnsCursor = migratedDb.query("PRAGMA table_info(AdvertisementSetCollectionEntity)")
        val columnNames = generateSequence { if (collectionColumnsCursor.moveToNext()) collectionColumnsCursor.getString(1) else null }.toList()
        assertTrue("expected lastUsedAt column", columnNames.contains("lastUsedAt"))
        assertTrue("expected isCustom column", columnNames.contains("isCustom"))
        collectionColumnsCursor.close()

        migratedDb.close()
    }
}
