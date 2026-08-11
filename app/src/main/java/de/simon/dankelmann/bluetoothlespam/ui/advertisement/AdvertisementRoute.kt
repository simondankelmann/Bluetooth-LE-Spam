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

    fun highlightCurrentAdvertisementSet(currentAdvertisementSet: AdvertisementSet, advertisementState: AdvertisementState) {
        advertisementSetLists.forEach { advertisementList ->
            advertisementList.currentlyAdvertising = false
            advertisementList.advertisementSets.forEach { advertisementSet ->
                if (advertisementSet == currentAdvertisementSet) {
                    advertisementSet.advertisementState = advertisementState
                    advertisementSet.currentlyAdvertising = true
                    advertisementList.currentlyAdvertising = true
                } else {
                    advertisementSet.currentlyAdvertising = false
                }
            }
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
