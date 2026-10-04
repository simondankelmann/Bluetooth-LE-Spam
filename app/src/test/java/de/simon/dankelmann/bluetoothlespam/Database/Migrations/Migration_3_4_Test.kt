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
 * CRITICAL regression test (plan §11 REGRESSION RULE) — an existing v3 install's
 * [de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity] rows
 * must survive the v3->v4 migration unchanged, and the two new Quick Start columns must land
 * exactly as Room expects (`runMigrationsAndValidate` fails loudly on any mismatch against the
 * exported schemas/4.json).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31], application = Application::class)
class Migration_3_4_Test {

    private val testDbName = "migration-3-4-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `existing AdvertisementSetCollection rows survive the v3 to v4 migration unchanged`() {
        val db = helper.createDatabase(testDbName, 3)
        db.execSQL(
            "INSERT INTO AdvertisementSetCollectionEntity (id, title, lastUsedAt, isCustom) " +
                "VALUES (1, 'Regression Test Collection', 12345, 1)",
        )
        db.close()

        // Runs Migration_3_4, then validates the resulting schema against schemas/4.json.
        val migratedDb = helper.runMigrationsAndValidate(testDbName, 4, true, Migration_3_4)

        val cursor = migratedDb.query("SELECT title, isCustom, isQuickStart, quickStartOrder FROM AdvertisementSetCollectionEntity WHERE id = 1")
        assertTrue("expected the pre-existing collection row to survive", cursor.moveToFirst())
        assertEquals("Regression Test Collection", cursor.getString(0))
        assertEquals(1, cursor.getInt(1))
        assertEquals("expected isQuickStart to default to false", 0, cursor.getInt(2))
        assertTrue("expected quickStartOrder to default to NULL", cursor.isNull(3))
        cursor.close()

        migratedDb.close()
    }
}
