package de.simon.dankelmann.bluetoothlespam.ui.advertisementcollection

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetCollectionEntity
import de.simon.dankelmann.bluetoothlespam.Database.builtInCollectionDefinitions
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.stringResId
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Route entry point (plan §6 step 4) — owns the business logic `AdvertisementCollectionFragment` used to own. */
@Composable
fun AdvertisementCollectionRoute(
    onNavigateToAdvertisement: () -> Unit,
    onCreateCustomGroup: () -> Unit,
) {
    val context = LocalContext.current
    var customGroups by remember { mutableStateOf<List<AdvertisementSetCollectionEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            customGroups = AppDatabase.getInstance().advertisementSetCollectionDao().getCustomCollections()
        }
    }

    AdvertisementCollectionScreen(
        premadeItems = remember { buildAdvertisementCollectionItems(context, onNavigateToAdvertisement) },
        customItems = customGroups.map { group ->
            AdvertisementCollectionItem(
                title = group.title,
                targetLabel = "Target: Mixed",
                distanceLabel = "Distance: Mixed",
                iconRes = R.drawable.list,
                onClick = { launchCustomGroup(context, group.id, onNavigateToAdvertisement) },
            )
        },
        onAddClicked = onCreateCustomGroup,
    )
}

/** Same skeleton-then-background-load shape as [navigateToAdvertisementWithType], for a saved custom group. */
private fun launchCustomGroup(context: Context, groupId: Int, onNavigateToAdvertisement: () -> Unit) {
    val app = context.applicationContext as BleSpamApplication
    Thread {
        val database = AppDatabase.getInstance()
        val collectionWithLists = database.advertisementSetCollectionDao().getCollectionWithLists(groupId)
            ?: return@Thread
        val listEntities = collectionWithLists.lists
        val collection = DatabaseHelpers.buildAdvertisementSetCollectionSkeletonFromEntity(collectionWithLists)
            .apply { isLoadingSets = true }

        Handler(Looper.getMainLooper()).post {
            app.queueHandler.apply {
                deactivate(context)
                setAdvertisementSetCollection(collection)
            }
            onNavigateToAdvertisement()

            Thread {
                val setsByList = listEntities.map {
                    de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper.filterDeleted(
                        DatabaseHelpers.getAllAdvertisementSetsForList(it.id)
                    )
                }
                Handler(Looper.getMainLooper()).post {
                    setsByList.forEachIndexed { index, sets ->
                        collection.advertisementSetLists[index].advertisementSets = sets.toMutableList()
                    }
                    collection.isLoadingSets = false
                    app.queueHandler.setAdvertisementSetCollection(collection)
                }
            }.start()
        }
    }.start()
}

/** UI-only metadata (icon/labels) per [builtInCollectionDefinitions] entry, keyed by title -- the
 * single source of truth for which types belong to each collection lives in that list. */
private data class CollectionCardMeta(val displayTitle: String, val targetLabel: String, val distanceLabel: String, val iconRes: Int)

private val builtInCollectionCardMeta: Map<String, CollectionCardMeta> = mapOf(
    "Fast Pair Collection" to CollectionCardMeta("Fast Pair", "Target: Android", "Distance: Close", R.drawable.ic_android),
    "Continuity Collection" to CollectionCardMeta("Continuity", "Target: iOS", "Distance: Mixed", R.drawable.apple),
    "Easy Setup Collection" to CollectionCardMeta("Easy Setup", "Target: Samsung", "Distance: Close", R.drawable.samsung),
    "Swift Pair Collection" to CollectionCardMeta("Swift Pair", "Target: Windows", "Distance: Close", R.drawable.microsoft),
    "Lovespouse Collection" to CollectionCardMeta("Lovespouse", "Target: Lovespouse", "Distance: Far", R.drawable.heart),
    "Kitchen Sink Collection" to CollectionCardMeta("Kitchen Sink", "Target: All", "Distance: Mixed", R.drawable.shuffle),
)

private fun buildAdvertisementCollectionItems(
    context: Context,
    onNavigateToAdvertisement: () -> Unit,
): List<AdvertisementCollectionItem> {
    fun navigateWithType(advertisementSetTypes: List<AdvertisementSetType>, title: String) {
        navigateToAdvertisementWithType(context, advertisementSetTypes, title, onNavigateToAdvertisement)
    }

    return builtInCollectionDefinitions.map { definition ->
        val meta = builtInCollectionCardMeta.getValue(definition.title)
        AdvertisementCollectionItem(
            title = meta.displayTitle,
            targetLabel = meta.targetLabel,
            distanceLabel = meta.distanceLabel,
            iconRes = meta.iconRes,
            onClick = {
                if (definition.title == "Kitchen Sink Collection") {
                    (context.applicationContext as BleSpamApplication).queueHandler
                        .setAdvertisementQueueMode(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM)
                }
                navigateWithType(definition.types, definition.title)
            },
        )
    }
}

/**
 * Navigates immediately with an empty-lists "skeleton" collection (title/hints/list-titles only
 * -- all synchronous, no DB access) so the control GUI shows up right away, then loads each
 * list's actual advertisement sets on a plain background Thread (must outlive this composable,
 * which unmounts the instant we navigate away -- a `rememberCoroutineScope()` job would be
 * cancelled at that point) and posts the result back to fill in the list section.
 */
private fun navigateToAdvertisementWithType(
    context: Context,
    advertisementSetTypes: List<AdvertisementSetType>,
    advertisementSetCollectionTitle: String,
    onNavigateToAdvertisement: () -> Unit,
) {
    val app = context.applicationContext as BleSpamApplication
    val advertisementSetCollection =
        buildAdvertisementCollectionSkeleton(context, advertisementSetTypes, advertisementSetCollectionTitle)
    advertisementSetCollection.isLoadingSets = true

    app.queueHandler.apply {
        deactivate(context)
        setAdvertisementSetCollection(advertisementSetCollection)
    }
    onNavigateToAdvertisement()

    Thread {
        val setsByType = advertisementSetTypes.map {
            de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper.filterDeleted(
                DatabaseHelpers.getAllAdvertisementSetsForType(it)
            )
        }
        Handler(Looper.getMainLooper()).post {
            setsByType.forEachIndexed { index, sets ->
                advertisementSetCollection.advertisementSetLists[index].advertisementSets = sets.toMutableList()
            }
            advertisementSetCollection.isLoadingSets = false
            app.queueHandler.setAdvertisementSetCollection(advertisementSetCollection)
        }
    }.start()
}

private fun buildAdvertisementCollectionSkeleton(
    context: Context,
    advertisementSetTypes: List<AdvertisementSetType>,
    advertisementSetCollectionTitle: String,
): AdvertisementSetCollection {
    val advertisementSetCollection = AdvertisementSetCollection()
    advertisementSetCollection.title = advertisementSetCollectionTitle

    if (advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEVICE) ||
        advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_PHONE_SETUP) ||
        advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_NON_PRODUCTION) ||
        advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_FAST_PAIRING_DEBUG)
    ) {
        advertisementSetCollection.hints.add("Fast Pairing is patched on all modern devices due to this we no longer offer support for this feature")
    }

    if (advertisementSetTypes.contains(AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH)) {
        advertisementSetCollection.hints.add("Devices on iOS 18 or above will not crash but still get pop-ups")
    }

    advertisementSetTypes.forEach { advertisementSetType ->
        val advertisementSetList = AdvertisementSetList()
        advertisementSetList.title = "${context.getString(advertisementSetType.stringResId())} List"
        advertisementSetCollection.advertisementSetLists.add(advertisementSetList)
    }

    return advertisementSetCollection
}
