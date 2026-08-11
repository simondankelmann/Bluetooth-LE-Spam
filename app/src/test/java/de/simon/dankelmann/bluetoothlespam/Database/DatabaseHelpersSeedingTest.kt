package de.simon.dankelmann.bluetoothlespam.Database

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the DAO queries + seeding logic activating the dormant AdvertisementSetList/
 * AdvertisementSetCollection schema (plan §8) — separate from [de.simon.dankelmann.bluetoothlespam.Database.Migrations.Migration_2_3_Test],
 * which only covers the CRITICAL existing-data-survives-migration path. This test asserts
 * against [AppDatabase.seedingThread]'s own auto-seed (fired from Room's onCreate callback on
 * first [AppDatabase.getInstance] access) rather than seeding again itself — calling
 * [DatabaseHelpers.seedBuiltInListsAndCollections] a second time would just double every count,
 * since both go through the same singleton.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 31], application = Application::class)
class DatabaseHelpersSeedingTest {

    @Test
    fun `seeding builds the 6 built-in collections with their member lists`() {
        AppContext.setContext(ApplicationProvider.getApplicationContext())

        var builtIns: List<AdvertisementSetCollectionEntity> = emptyList()
        var fastPairListCount = -1
        var fastPairListTitlesBlank = true
        var fastPairDeviceListId = -1
        var kitchenSinkContainsFastPairDeviceList = false
        var fastPairDeviceSetCount = 0
        var fastPairDeviceListSetCount = 0
        var customCollectionCountBeforeUse = -1
        var recentlyUsedCountBeforeUse = -1
        var recentlyUsedAfterUse: List<AdvertisementSetCollectionEntity> = emptyList()

        // Room refuses DB access on the "main thread", which Robolectric's test thread is —
        // real app code always does this seeding/querying from a background Thread too (see
        // AppDatabase.seedingThread / Migration_2_3), so match that here rather than special-
        // casing the test database. All DB work (writes AND reads) happens in this one thread;
        // assertions run afterward on the main thread against the captured results.
        val workerThread = Thread {
            // AppDatabase.getInstance() opens a fresh db on first access, which triggers Room's
            // onCreate callback (AppDatabase.seedingThread) on its own background Thread — it
            // already seeds every generator (including FastPairDevicesAdvertisementSetGenerator)
            // and calls DatabaseHelpers.seedBuiltInListsAndCollections() itself, so seeding again
            // here would just double every count. Force the db open and wait for that auto-seed
            // to finish, then assert against its result instead of re-seeding.
            val database = AppDatabase.getInstance()
            database.advertisementSetDao().getAll()
            var waitedMillis = 0L
            while (database.isSeeding && waitedMillis < 5000) {
                Thread.sleep(20)
                waitedMillis += 20
            }

            builtIns = database.advertisementSetCollectionDao().getBuiltInCollections()

            val fastPair = builtIns.first { it.title == "Fast Pair Collection" }
            val fastPairWithLists = database.advertisementSetCollectionDao().getCollectionWithLists(fastPair.id)
            fastPairListCount = fastPairWithLists.lists.size
            fastPairListTitlesBlank = fastPairWithLists.lists.any { it.title.isBlank() }

            val fastPairDeviceList = fastPairWithLists.lists.first { it.title == "Fast Pairing Device List" }
            fastPairDeviceListId = fastPairDeviceList.id

            val kitchenSink = builtIns.first { it.title == "Kitchen Sink Collection" }
            val kitchenSinkWithLists = database.advertisementSetCollectionDao().getCollectionWithLists(kitchenSink.id)
            kitchenSinkContainsFastPairDeviceList = kitchenSinkWithLists.lists.any { it.id == fastPairDeviceListId }

            fastPairDeviceSetCount = database.advertisementSetDao()
                .findByType(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE).size
            fastPairDeviceListSetCount = database.associationListSetDao().getAll()
                .count { it.advertisementSetListId == fastPairDeviceListId }

            customCollectionCountBeforeUse = database.advertisementSetCollectionDao().getCustomCollections().size
            recentlyUsedCountBeforeUse = database.advertisementSetCollectionDao().getRecentlyUsed().size

            database.advertisementSetCollectionDao().updateLastUsedAt(fastPair.id, 12345L)
            recentlyUsedAfterUse = database.advertisementSetCollectionDao().getRecentlyUsed()
        }
        workerThread.start()
        workerThread.join()

        assertEquals(6, builtIns.size)
        assertTrue(builtIns.none { it.isCustom })
        assertEquals("Fast Pair Collection should have all 4 of its member lists", 4, fastPairListCount)
        assertTrue(!fastPairListTitlesBlank)
        assertTrue("Kitchen Sink should reuse Fast Pair's list rows, not copy them", kitchenSinkContainsFastPairDeviceList)
        assertEquals(
            "every FastPairingDevice set should be associated to the Fast Pairing Device type's list",
            fastPairDeviceSetCount,
            fastPairDeviceListSetCount,
        )
        assertTrue("expected at least one FastPairingDevice set to be seeded", fastPairDeviceSetCount > 0)
        assertEquals(0, customCollectionCountBeforeUse)
        assertEquals(0, recentlyUsedCountBeforeUse)
        assertEquals(1, recentlyUsedAfterUse.size)
        assertEquals("Fast Pair Collection", recentlyUsedAfterUse.single().title)
    }
}
