package de.simon.dankelmann.bluetoothlespam.Database

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [DatabaseHelpers.syncBuiltInSets] — the additive top-up that lets an existing install
 * pick up built-in model IDs added since it first seeded (called from `BleSpamApplication` when
 * the stored built-in-seed version is behind). Asserts the two properties that keep it safe to
 * run on every upgrade: it re-adds missing built-in sets to their type's list, and it never
 * duplicates sets that are already present (idempotent).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31], application = Application::class)
class DatabaseHelpersSyncTest {

    private val fastPairType = AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE

    @Test
    fun `syncBuiltInSets is idempotent and re-adds missing built-in sets`() {
        AppContext.setContext(ApplicationProvider.getApplicationContext())

        var seededCount = -1
        var countAfterNoopSync = -1
        var assocAfterNoopSync = -1
        var countAfterDeletion = -1
        var countAfterRestoreSync = -1
        var assocAfterRestoreSync = -1
        var countAfterSecondSync = -1
        var assocAfterSecondSync = -1

        // Room refuses DB access on the test (main) thread; real app code seeds/syncs from a
        // background Thread too, so match that here (see DatabaseHelpersSeedingTest).
        val workerThread = Thread {
            val database = AppDatabase.getInstance()
            // Force the db open (fires onCreate auto-seed on its own Thread) and wait it out.
            database.advertisementSetDao().getAll()
            var waitedMillis = 0L
            while (database.isSeeding && waitedMillis < 5000) {
                Thread.sleep(20)
                waitedMillis += 20
            }

            val listId = database.advertisementSetListDao().getAll()
                .first { it.title == "Fast Pairing Device List" }.id

            fun fastPairSetCount() = database.advertisementSetDao().findByType(fastPairType).size
            fun fastPairAssocCount() =
                database.associationListSetDao().getAll().count { it.advertisementSetListId == listId }

            seededCount = fastPairSetCount()

            // Already fully seeded, so a sync must be a no-op.
            DatabaseHelpers.syncBuiltInSets()
            countAfterNoopSync = fastPairSetCount()
            assocAfterNoopSync = fastPairAssocCount()

            // Simulate an older install that is missing some built-in sets: drop a handful of
            // Fast Pair sets and their list associations.
            val toRemove = database.advertisementSetDao().findByType(fastPairType).take(5)
            val associations = database.associationListSetDao().getAll()
            toRemove.forEach { set ->
                associations.filter { it.advertisementSetId == set.id }
                    .forEach { database.associationListSetDao().delete(it) }
                database.advertisementSetDao().delete(set)
            }
            countAfterDeletion = fastPairSetCount()

            // Sync should re-add exactly the removed sets (matched by payload, not id).
            DatabaseHelpers.syncBuiltInSets()
            countAfterRestoreSync = fastPairSetCount()
            assocAfterRestoreSync = fastPairAssocCount()

            // A further sync must add nothing (idempotent — this is what runs on normal launches).
            DatabaseHelpers.syncBuiltInSets()
            countAfterSecondSync = fastPairSetCount()
            assocAfterSecondSync = fastPairAssocCount()
        }
        workerThread.start()
        workerThread.join()

        assertTrue("expected Fast Pair devices to be seeded", seededCount > 5)
        assertEquals("sync on an already-seeded db must not add sets", seededCount, countAfterNoopSync)
        assertEquals("every set stays associated to its type's list", seededCount, assocAfterNoopSync)

        assertEquals("deletion should remove exactly 5 sets", seededCount - 5, countAfterDeletion)

        assertEquals("sync must re-add the missing sets", seededCount, countAfterRestoreSync)
        assertEquals("re-added sets must be re-associated to the list", seededCount, assocAfterRestoreSync)

        assertEquals("a second sync must add nothing (no duplicates)", seededCount, countAfterSecondSync)
        assertEquals("a second sync must not duplicate associations", seededCount, assocAfterSecondSync)
    }
}
