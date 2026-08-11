package de.simon.dankelmann.bluetoothlespam.ui.deviceselector

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Route entry point (plan §8) — loads real lists/groups from the DB, relaunches on selection. */
@Composable
fun DeviceSelectorRoute(
    onNavigateToAdvertisement: () -> Unit,
    onNavigateToGroupEditor: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lists by remember { mutableStateOf<List<AdvertisementSetListEntity>>(emptyList()) }
    var builtInGroups by remember { mutableStateOf<List<AdvertisementSetCollectionEntity>>(emptyList()) }
    var customGroups by remember { mutableStateOf<List<AdvertisementSetCollectionEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getInstance()
            lists = database.advertisementSetListDao().getAll()
            builtInGroups = database.advertisementSetCollectionDao().getBuiltInCollections()
            customGroups = database.advertisementSetCollectionDao().getCustomCollections()
        }
    }

    DeviceSelectorScreen(
        lists = lists,
        builtInGroups = builtInGroups,
        customGroups = customGroups,
        onListSelected = { list -> launchList(context, list, onNavigateToAdvertisement) },
        onGroupSelected = { group -> launchGroup(context, scope, group, onNavigateToAdvertisement) },
        onCreateCustomGroupClicked = onNavigateToGroupEditor,
    )
}

/**
 * Navigates immediately with a one-list "skeleton" collection (title only, no DB access needed
 * since [list] is already loaded) so the control GUI shows up right away, then loads the list's
 * actual advertisement sets on a plain background Thread and posts the result back to fill in
 * the list section (plan §8's "loader while lists load" UX, mirrors AdvertisementCollectionRoute).
 */
private fun launchList(
    context: Context,
    list: AdvertisementSetListEntity,
    onNavigateToAdvertisement: () -> Unit,
) {
    val app = context.applicationContext as BleSpamApplication
    val collection = AdvertisementSetCollection().apply {
        title = list.title
        advertisementSetLists.add(AdvertisementSetList().apply { title = list.title })
        isLoadingSets = true
    }

    app.queueHandler.apply {
        deactivate(context)
        setAdvertisementSetCollection(collection)
    }
    onNavigateToAdvertisement()

    Thread {
        val sets = DatabaseHelpers.getAllAdvertisementSetsForList(list.id)
        Handler(Looper.getMainLooper()).post {
            collection.advertisementSetLists[0].advertisementSets = sets.toMutableList()
            collection.isLoadingSets = false
            app.queueHandler.setAdvertisementSetCollection(collection)
        }
    }.start()
}

/**
 * Same idea as [launchList], but a group's member lists aren't known until [getCollectionWithLists]
 * returns, so that (fast, single-query) lookup stays on the pre-navigation IO hop; only the
 * potentially-many-lists set loading moves to the background Thread.
 */
private fun launchGroup(
    context: Context,
    scope: CoroutineScope,
    group: AdvertisementSetCollectionEntity,
    onNavigateToAdvertisement: () -> Unit,
) {
    scope.launch(Dispatchers.IO) {
        val database = AppDatabase.getInstance()
        val collectionWithLists = database.advertisementSetCollectionDao().getCollectionWithLists(group.id)
        database.advertisementSetCollectionDao().updateLastUsedAt(group.id, System.currentTimeMillis())

        val listEntities = collectionWithLists.lists
        val collection = DatabaseHelpers.buildAdvertisementSetCollectionSkeletonFromEntity(collectionWithLists)
            .apply { isLoadingSets = true }

        withContext(Dispatchers.Main) {
            val app = context.applicationContext as BleSpamApplication
            app.queueHandler.apply {
                deactivate(context)
                setAdvertisementSetCollection(collection)
            }
            onNavigateToAdvertisement()

            Thread {
                val setsByList = listEntities.map { DatabaseHelpers.getAllAdvertisementSetsForList(it.id) }
                Handler(Looper.getMainLooper()).post {
                    setsByList.forEachIndexed { index, sets ->
                        collection.advertisementSetLists[index].advertisementSets = sets.toMutableList()
                    }
                    collection.isLoadingSets = false
                    app.queueHandler.setAdvertisementSetCollection(collection)
                }
            }.start()
        }
    }
}
