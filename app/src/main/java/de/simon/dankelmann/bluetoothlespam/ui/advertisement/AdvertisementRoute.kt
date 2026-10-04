package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsKeys
import de.simon.dankelmann.bluetoothlespam.Datastore.SettingsRepository
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementState
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementSetQueueHandlerCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LOG_TAG = "AdvertisementRoute"

/** Route entry point (plan §6 step 4) — owns the business logic `AdvertisementFragment` used to own. */
@Composable
fun AdvertisementRoute() {
    val context = LocalContext.current
    val viewModel: AdvertisementViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val settings by settingsRepository.preferencesFlow.collectAsState()
    val allowCustomSwiftPairNames = settings[SettingsKeys.ALLOW_CUSTOM_SWIFT_PAIR_NAMES] ?: false

    var advertisementSetLists by remember { mutableStateOf<List<AdvertisementSetList>>(emptyList()) }
    var revision by remember { mutableIntStateOf(0) }
    // Last highlighted row: plain holder (not State) so updating it doesn't trigger an extra
    // recomposition on top of the `revision++` below. Lets highlight reset O(1) instead of
    // clearing all ~1000 rows on every spam tick.
    val lastHighlight = remember { object { var list: AdvertisementSetList? = null; var set: AdvertisementSet? = null } }

    fun highlightCurrentAdvertisementSet(currentAdvertisementSet: AdvertisementSet, advertisementState: AdvertisementState) {
        lastHighlight.set?.currentlyAdvertising = false
        val prevList = lastHighlight.list
        // Only clear the previous list flag if the new set lives in another list;
        // it will be re-set below when found.
        var foundList: AdvertisementSetList? = null
        var found = false
        for (advertisementList in advertisementSetLists) {
            if (found) break
            for (advertisementSet in advertisementList.advertisementSets) {
                if (advertisementSet == currentAdvertisementSet) {
                    advertisementSet.advertisementState = advertisementState
                    advertisementSet.currentlyAdvertising = true
                    advertisementList.currentlyAdvertising = true
                    foundList = advertisementList
                    found = true
                    break
                }
            }
        }
        if (prevList != null && prevList != foundList) {
            prevList.currentlyAdvertising = false
        }
        if (found) {
            lastHighlight.list = foundList
            lastHighlight.set = currentAdvertisementSet
        }
        revision++
    }

    val callback = remember {
        object : IAdvertisementServiceCallback, IAdvertisementSetQueueHandlerCallback {
            override fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?) {
                Log.d(LOG_TAG, "onAdvertisementSetStart ${advertisementSet?.title}")
                if (advertisementSet != null) {
                    viewModel.target.value = advertisementSet.target
                    viewModel.advertisementSetTitle.value = advertisementSet.title
                    viewModel.advertisementSetSubTitle.value = advertisementSetSubtitle(advertisementSet)
                    highlightCurrentAdvertisementSet(advertisementSet, AdvertisementState.ADVERTISEMENT_STATE_STARTED)
                }
            }

            override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
                Log.d(LOG_TAG, "onAdvertisementSetStop")
            }

            override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
                if (advertisementSet != null) {
                    highlightCurrentAdvertisementSet(advertisementSet, AdvertisementState.ADVERTISEMENT_STATE_SUCCEEDED)
                }
            }

            override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
                if (advertisementSet != null) {
                    highlightCurrentAdvertisementSet(advertisementSet, AdvertisementState.ADVERTISEMENT_STATE_FAILED)
                    Toast.makeText(context, "Advertisement Failed: $advertisementError", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onQueueHandlerActivated() {
                Log.d(LOG_TAG, "onQueueHandlerActivated")
                viewModel.isAdvertising.value = true
            }

            override fun onQueueHandlerDeactivated() {
                Log.d(LOG_TAG, "onQueueHandlerDeactivated")
                viewModel.isAdvertising.value = false
            }

            override fun onAdvertisementSetCollectionChanged() {
                val queue = (context.applicationContext as BleSpamApplication).queueHandler
                val collection = queue.getAdvertisementSetCollection()
                viewModel.isLoadingSets.value = collection.isLoadingSets
                viewModel.advertisementSetCollectionTitle.value = collection.title
                viewModel.advertisementSetCollectionSubTitle.value = advertisementSetCollectionSubTitle(collection)
                viewModel.advertisementSetCollectionHint.value = advertisementSetCollectionHint(collection)
                lastHighlight.list = null
                lastHighlight.set = null
                advertisementSetLists = collection.advertisementSetLists.toList()
                revision++
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val queue = (context.applicationContext as BleSpamApplication).queueHandler
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    queue.addAdvertisementServiceCallback(callback)
                    queue.addAdvertisementQueueHandlerCallback(callback)

                    viewModel.advertisementQueueMode.value = queue.getAdvertisementQueueMode()
                    viewModel.isAdvertising.value = queue.isActive()

                    val collection = queue.getAdvertisementSetCollection()
                    viewModel.isLoadingSets.value = collection.isLoadingSets
                    viewModel.advertisementSetCollectionTitle.value = collection.title
                    viewModel.advertisementSetCollectionSubTitle.value = advertisementSetCollectionSubTitle(collection)
                    viewModel.advertisementSetCollectionHint.value = advertisementSetCollectionHint(collection)

                    lastHighlight.list = null
                    lastHighlight.set = null
                    advertisementSetLists = collection.advertisementSetLists.toList()
                    revision++
                }
                Lifecycle.Event.ON_PAUSE -> {
                    queue.removeAdvertisementServiceCallback(callback)
                    queue.removeAdvertisementQueueHandlerCallback(callback)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isAdvertising by viewModel.isAdvertising.observeAsState(false)
    val target by viewModel.target.observeAsState(viewModel.target.value!!)
    val collectionTitle by viewModel.advertisementSetCollectionTitle.observeAsState("-")
    val collectionSubtitle by viewModel.advertisementSetCollectionSubTitle.observeAsState("-")
    val collectionHint by viewModel.advertisementSetCollectionHint.observeAsState("-")
    val currentSetTitle by viewModel.advertisementSetTitle.observeAsState("-")
    val currentSetSubtitle by viewModel.advertisementSetSubTitle.observeAsState("-")
    val queueMode by viewModel.advertisementQueueMode.observeAsState(AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM)
    val isLoadingSets by viewModel.isLoadingSets.observeAsState(false)

    // Presence of a SwiftPair list only changes when the collection instance changes,
    // never on `revision` ticks (checkbox/highlight mutations) — so don't rescan ~1000 rows
    // on every spam tick.
    val showCustomSwiftPairAdd = remember(advertisementSetLists, allowCustomSwiftPairNames) {
        allowCustomSwiftPairNames && advertisementSetLists.any { list ->
            list.advertisementSets.any {
                it.type == de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING
            }
        }
    }

    fun reloadListsFilteringDeleted() {
        // Remove soft-deleted sets from the in-memory collection so UI updates instantly
        val deleted = de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper.getDeletedIds()
        if (deleted.isEmpty()) return
        advertisementSetLists.forEach { list ->
            list.advertisementSets.removeAll { deleted.contains(it.id) }
        }
    }

    AdvertisementScreen(
        isAdvertising = isAdvertising == true,
        target = target,
        collectionTitle = collectionTitle ?: "-",
        collectionSubtitle = collectionSubtitle ?: "-",
        collectionHint = collectionHint ?: "-",
        currentSetTitle = currentSetTitle ?: "-",
        currentSetSubtitle = currentSetSubtitle ?: "-",
        queueMode = queueMode ?: AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM,
        isLoadingSets = isLoadingSets == true,
        advertisementSetLists = advertisementSetLists,
        revision = revision,
        onPlayClicked = { onPlayButtonClicked(context, viewModel) },
        onQueueModeSelected = { mode -> setAdvertisementQueueMode(context, viewModel, mode) },
        onSetRowClicked = { groupIndex, childIndex, set ->
            (context.applicationContext as BleSpamApplication).queueHandler.setSelectedAdvertisementSet(groupIndex, childIndex)
            highlightCurrentAdvertisementSet(set, AdvertisementState.ADVERTISEMENT_STATE_UNDEFINED)
        },
        onSetCheckedChanged = { set, checked ->
            set.isChecked = checked
            revision++
        },
        onGroupCheckedChanged = { list, checked ->
            list.advertisementSets.forEach { it.isChecked = checked }
            revision++
        },
        allowCustomSwiftPairNames = allowCustomSwiftPairNames,
        onRenameSwiftPairDevice = { set, newName ->
            scope.launch(Dispatchers.IO) {
                DatabaseHelpers.updateSwiftPairDeviceName(set, newName)
                withContext(Dispatchers.Main) { revision++ }
            }
        },
        onEditDevice = { set ->
            // View-based generic editor (title + manufacturer/service hex + flags).
            // Must run on UI thread for dialog; DB load already in memory.
            scope.launch(Dispatchers.IO) {
                val full = DatabaseHelpers.getAdvertisementSetById(set.id) ?: set
                withContext(Dispatchers.Main) {
                    try {
                        EditAdvertisementSetDialog.show(context, full) { edited ->
                            scope.launch(Dispatchers.IO) {
                                try {
                                    de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper.saveBackupIfAbsent(full)
                                    full.title = edited.title
                                    full.advertiseData.includeDeviceName = edited.includeDeviceName
                                    full.advertiseData.includeTxPower = edited.includeTxPower
                                    edited.manufacturerIds.forEachIndexed { i, mid ->
                                        full.advertiseData.manufacturerData.getOrNull(i)?.let {
                                            it.manufacturerId = mid
                                            it.manufacturerSpecificData =
                                                de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.decodeHex(edited.manufacturerHex[i])
                                        }
                                    }
                                    edited.serviceUuids.forEachIndexed { i, uuidRaw ->
                                        full.advertiseData.services.getOrNull(i)?.let {
                                            it.serviceUuid = android.os.ParcelUuid.fromString(uuidRaw)
                                            val hex = edited.serviceHex.getOrNull(i)
                                            it.serviceData = if (hex == null) null
                                            else de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.decodeHex(hex)
                                        }
                                    }
                                    DatabaseHelpers.updateAdvertisementSetContent(full)
                                    if (de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper.matchesBackup(full)) {
                                        de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper.clearBackup(full.id)
                                    }
                                    // Reflect in-memory so list updates without reload
                                    set.title = full.title
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, context.getString(de.simon.dankelmann.bluetoothlespam.R.string.device_updated), Toast.LENGTH_SHORT).show()
                                        revision++
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Edit failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Edit failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        onDeleteDevice = { set ->
            try {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                    .setTitle(context.getString(de.simon.dankelmann.bluetoothlespam.R.string.device_delete_title))
                    .setMessage(context.getString(de.simon.dankelmann.bluetoothlespam.R.string.device_delete_message, set.title))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        scope.launch(Dispatchers.IO) {
                            de.simon.dankelmann.bluetoothlespam.Helpers.DeviceCustomizationHelper.setDeleted(set.id, true)
                            withContext(Dispatchers.Main) {
                                advertisementSetLists.forEach { list ->
                                    list.advertisementSets.removeAll { it.id == set.id }
                                }
                                Toast.makeText(context, context.getString(de.simon.dankelmann.bluetoothlespam.R.string.device_deleted), Toast.LENGTH_SHORT).show()
                                revision++
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        },
        showCustomSwiftPairAdd = showCustomSwiftPairAdd,
        onAddCustomSwiftPair = { name ->
            scope.launch(Dispatchers.IO) {
                try {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(de.simon.dankelmann.bluetoothlespam.R.string.swift_pair_custom_empty), Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    if (trimmed.toByteArray(Charsets.UTF_8).size > 24) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(de.simon.dankelmann.bluetoothlespam.R.string.swift_pair_custom_too_long), Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    val duplicate = advertisementSetLists.flatMap { it.advertisementSets }
                        .any { it.title.equals(trimmed, ignoreCase = true) }
                    if (duplicate) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(de.simon.dankelmann.bluetoothlespam.R.string.swift_pair_custom_exists), Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    val generator = de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.SwiftPairAdvertisementSetGenerator()
                    val newSet = generator.getAdvertisementSets(mapOf(trimmed to "Not used...")).firstOrNull()
                    if (newSet != null) {
                        val newId = DatabaseHelpers.saveAdvertisementSetAndAssociate(newSet)
                        if (newId > 0) {
                            newSet.id = newId
                            newSet.isChecked = true
                            withContext(Dispatchers.Main) {
                                // Append to first SwiftPair list in memory
                                advertisementSetLists.forEach { list ->
                                    if (list.advertisementSets.any { it.type == de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType.ADVERTISEMENT_TYPE_SWIFT_PAIRING }) {
                                        if (list.advertisementSets.none { it.id == newId }) {
                                            list.advertisementSets.add(newSet)
                                        }
                                    }
                                }
                                Toast.makeText(context, context.getString(de.simon.dankelmann.bluetoothlespam.R.string.swift_pair_custom_added), Toast.LENGTH_SHORT).show()
                                revision++
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Add failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
    )
}

private fun onPlayButtonClicked(context: Context, viewModel: AdvertisementViewModel) {
    val queue = (context.applicationContext as BleSpamApplication).queueHandler
    if (viewModel.isAdvertising.value == true) {
        queue.deactivate(context)
    } else {
        queue.activate(context)
    }
}

private fun setAdvertisementQueueMode(context: Context, viewModel: AdvertisementViewModel, mode: AdvertisementQueueMode) {
    (context.applicationContext as BleSpamApplication).queueHandler.setAdvertisementQueueMode(mode)
    viewModel.advertisementQueueMode.value = mode
}

private fun advertisementSetCollectionSubTitle(advertisementSetCollection: AdvertisementSetCollection): String {
    return "${advertisementSetCollection.getTotalNumberOfAdvertisementSets()} Devices in ${advertisementSetCollection.getNumberOfLists()} Lists"
}

private fun advertisementSetCollectionHint(advertisementSetCollection: AdvertisementSetCollection): String {
    if (advertisementSetCollection.hints.isEmpty()) return "-"
    return advertisementSetCollection.hints.joinToString(", ")
}
