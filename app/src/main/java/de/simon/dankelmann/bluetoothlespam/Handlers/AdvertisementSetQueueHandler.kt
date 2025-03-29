package de.simon.dankelmann.bluetoothlespam.Handlers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityActionModalAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityIos17CrashAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewAirtagPopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNewDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.ContinuityNotYourDevicePopUpAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementSetType
import de.simon.dankelmann.bluetoothlespam.Enums.TxPowerLevel
import de.simon.dankelmann.bluetoothlespam.Helpers.QueueHandlerHelpers
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementSetQueueHandlerCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.Services.AdvertisementForegroundService
import kotlin.random.Random

/**
 * Handler that takes an advertisement set, and iterates over the set according to a given AdvertisementQueueMode.
 *
 * The job of this handler is to select the next set, and provide it to the IAdvertisementService.
 *
 * The UI code should drive the advertising via this handler (via start, stop, set advertisement set, set queue mode).
 * This handler takes care of starting and stopping services as appropriate.
 */
class AdvertisementSetQueueHandler(
    context: Context,
    adService: IAdvertisementService,
) : IAdvertisementServiceCallback {

    private var _logTag = "AdvertisementSetQueueHandler"

    private var _advertisementService: IAdvertisementService = adService

    private var _advertisementQueueMode: AdvertisementQueueMode = AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR
    private var _advertisementSetCollection: AdvertisementSetCollection =
        AdvertisementSetCollection()
    private var _intervalMillis: Long = QueueHandlerHelpers.getInterval(context)

    // Callbacks to listen to events of the underlying advertisement service
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    // Callbacks to listen to queue events
    private var _advertisementQueueHandlerCallbacks:MutableList<IAdvertisementSetQueueHandlerCallback> = mutableListOf()

    private var _active = false
    private var _currentAdvertisementSet: AdvertisementSet? = null
    private var _currentAdvertisementSetListIndex = 0
    private var _currentAdvertisementSetIndex = 0

    init {
        _advertisementService.addAdvertisementServiceCallback(this)
    }

    fun isActive(): Boolean {
        return _active
    }

    fun setAdvertisementQueueMode(advertisementQueueMode: AdvertisementQueueMode){
        _advertisementQueueMode = advertisementQueueMode
    }

    fun getAdvertisementQueueMode():AdvertisementQueueMode{
        return _advertisementQueueMode
    }

    fun setInterval(milliseconds: Long) {
        if (milliseconds > 0) {
            _intervalMillis = milliseconds
        }
    }

    fun setAdvertisementService(advertisementService: IAdvertisementService) {
        _advertisementService.removeAdvertisementServiceCallback(this)

        _advertisementService = advertisementService
        _advertisementService.addAdvertisementServiceCallback(this)
    }


    fun setSelectedAdvertisementSet(advertisementSetListIndex: Int, advertisementSetIndex: Int){
        val advertisementSet = _advertisementSetCollection.advertisementSetLists[advertisementSetListIndex]?.advertisementSets?.get(advertisementSetIndex)
        if (advertisementSet != null) {
            _currentAdvertisementSetListIndex = advertisementSetListIndex
            _currentAdvertisementSetIndex = advertisementSetIndex
            _currentAdvertisementSet = advertisementSet
        }
    }

    fun setAdvertisementSetCollection(advertisementSetCollection: AdvertisementSetCollection){
        if(_advertisementSetCollection != advertisementSetCollection){
            _advertisementSetCollection = advertisementSetCollection
        }

        // Reset indices
        _currentAdvertisementSet= null
        _currentAdvertisementSetListIndex = 0
        _currentAdvertisementSetIndex = 0
    }

    fun getAdvertisementSetCollection(): AdvertisementSetCollection{
        return _advertisementSetCollection
    }

    // Add / Remove AdvertisementSetCollections
    fun clearAdvertisementSetCollection(){
        _advertisementSetCollection.advertisementSetLists.clear()
    }
    fun addAdvertisementSetList(advertisementSetList: AdvertisementSetList){
        if(!_advertisementSetCollection.advertisementSetLists.contains(advertisementSetList)){
            _advertisementSetCollection.advertisementSetLists.add(advertisementSetList)
        }
    }

    fun removeAdvertisementSetList(advertisementSetList: AdvertisementSetList){
        if(_advertisementSetCollection.advertisementSetLists.contains(advertisementSetList)){
            _advertisementSetCollection.advertisementSetLists.remove(advertisementSetList)
        }
    }

    // Add / Remove Callbacks
    fun addAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(!_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.add(callback)
        }
    }
    fun removeAdvertisementServiceCallback(callback: IAdvertisementServiceCallback){
        if(_advertisementServiceCallbacks.contains(callback)){
            _advertisementServiceCallbacks.remove(callback)
        }
    }

    fun addAdvertisementQueueHandlerCallback(callback: IAdvertisementSetQueueHandlerCallback){
        if(!_advertisementQueueHandlerCallbacks.contains(callback)){
            _advertisementQueueHandlerCallbacks.add(callback)
        }
    }
    fun removeAdvertisementQueueHandlerCallback(callback: IAdvertisementSetQueueHandlerCallback){
        if(_advertisementQueueHandlerCallbacks.contains(callback)){
            _advertisementQueueHandlerCallbacks.remove(callback)
        }
    }

    fun hasCheckedItems(): Boolean {
        // Check if any advertisement set is checked
        for (list in _advertisementSetCollection.advertisementSetLists) {
            for (set in list.advertisementSets) {
                if (set.isChecked) {
                    return true
                }
            }
        }
        return false
    }

    fun activate(context: Context) {
        if (_active) {
            return
        }

        // Check if any items are selected before activating
        if (!hasCheckedItems()) {
            // Show toast message if no items are selected
            android.widget.Toast.makeText(context, "No items selected", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        _active = true
        AdvertisementForegroundService.startService(context)
        _advertisementQueueHandlerCallbacks.forEach { it ->
            try {
                it.onQueueHandlerActivated()
            } catch (e: Exception) {
                Log.e(_logTag, "Failed to call onQueueHandlerActivated: ${e.message}")
            }
        }
        advertiseNextAdvertisementSet()
    }

    fun deactivate(context: Context, stopService: Boolean = false) {
        _active = false

        _advertisementService.stopAdvertisement()

        if (stopService) {
            Log.d(_logTag, "Stopping Foreground Service")
            AdvertisementForegroundService.stopService(context)
        }

        _advertisementQueueHandlerCallbacks.forEach { it ->
            try {
                it.onQueueHandlerDeactivated()
            } catch (e: Exception) {
                Log.e(_logTag, "Failed to call onQueueHandlerDeactivated: ${e.message}")
            }
        }
    }

    private fun advertiseNextAdvertisementSet() {
        selectNextAdvertisementSet()

        val nextSet = _currentAdvertisementSet
        if (nextSet == null) {
            Log.e(_logTag, "Current Advertisement Set is null.")
            return
        }

        if (_active) {
            // Only advertise if the set is checked
            if (nextSet.isChecked) {
                val preparedSet = prepareAdvertisementSet(nextSet)
                _advertisementService.startAdvertisement(preparedSet)
            } else {
                // If the set is not checked, immediately move to the next one
                Log.d(_logTag, "Skipping unchecked advertisement set: ${nextSet.title}")
                onAdvertisementSucceeded()
            }
        }
    }

    private fun prepareAdvertisementSet(advertisementSet: AdvertisementSet): AdvertisementSet {
        return when (advertisementSet.type) {
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> ContinuityNewDevicePopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> ContinuityNewAirtagPopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> ContinuityNotYourDevicePopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> ContinuityActionModalAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> ContinuityIos17CrashAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            else -> advertisementSet
        }
    }

    /**
     * Select the AdvertisementSet that should be advertised next.
     *
     * Precondition: at least one set is checked by the user.
     * The case of nothing being checked should be handled earlier.
     * If nothing is checked, this function will do nothing.
     */
    private fun selectNextAdvertisementSet() {
        // Explicit returns are used for clarity

        when (_advertisementQueueMode) {
            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR -> {
                // If no AdvertisementSet is currently selected, make sure to start at the beginning
                if (_currentAdvertisementSet == null) {
                    _currentAdvertisementSetListIndex = 0
                    _currentAdvertisementSetIndex = 0
                }

                val selectedList =
                    _advertisementSetCollection.advertisementSetLists[_currentAdvertisementSetListIndex]
                Log.d(
                    _logTag,
                    "List: ${selectedList.title}, SETS: ${selectedList.advertisementSets.count()}, CurrentIndex: $_currentAdvertisementSetIndex"
                )

                // Find the next checked item in the current list
                for (i in (_currentAdvertisementSetIndex + 1) until selectedList.advertisementSets.size) {
                    if (selectedList.advertisementSets[i].isChecked) {
                        // _currentAdvertisementSetListIndex is unchanged
                        _currentAdvertisementSetIndex = i
                        _currentAdvertisementSet = selectedList.advertisementSets[i]
                        return
                    }
                }

                // If we didn't find a checked item in the current list, move to the next list
                // Find the next list with checked items
                val startListIndex = _currentAdvertisementSetListIndex
                val numberOfLists = _advertisementSetCollection.advertisementSetLists.size

                // Loop through lists starting from the next one
                for (listOffset in 1..numberOfLists) {
                    val listIndex = (startListIndex + listOffset) % numberOfLists
                    val list = _advertisementSetCollection.advertisementSetLists[listIndex]

                    // Find the first checked item in this list
                    val firstCheckedIndex = list.advertisementSets.indexOfFirst { it.isChecked }
                    if (firstCheckedIndex >= 0) {
                        _currentAdvertisementSetListIndex = listIndex
                        _currentAdvertisementSetIndex = firstCheckedIndex
                        _currentAdvertisementSet = list.advertisementSets[firstCheckedIndex]
                        return
                    }
                }

                // No checked set found in any list
                return
            }

            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM -> {
                // Create a list of all checked advertisement sets across all lists
                // TODO: Cache this, don't recompute it all the time?
                val checkedSets = mutableListOf<Triple<Int, Int, AdvertisementSet>>()

                _advertisementSetCollection.advertisementSetLists.forEachIndexed { listIndex, list ->
                    list.advertisementSets.forEachIndexed { setIndex, set ->
                        if (set.isChecked) {
                            checkedSets.add(Triple(listIndex, setIndex, set))
                        }
                    }
                }

                // If we have checked items, randomly select one of them
                if (checkedSets.isNotEmpty()) {
                    val randomIndex = Random.nextInt(checkedSets.size)
                    val selected = checkedSets[randomIndex]
                    _currentAdvertisementSetListIndex = selected.first
                    _currentAdvertisementSetIndex = selected.second
                    _currentAdvertisementSet = selected.third
                } else {
                    // If no checked items, do nothing
                }
            }
        }
    }

    private fun onAdvertisementSucceeded() {
        _advertisementService.stopAdvertisement()

        if (_advertisementService.isLegacyService()) {
            advertiseNextAdvertisementSet()
        } else {
            // Wait for the Stop Advertising Callback
        }
    }

    private fun onAdvertisementFailed() {
        Log.d(_logTag, "Advertisement failed, trying again")
        onAdvertisementSucceeded()
    }

    private fun runLocalCallback(success:Boolean){
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                if(success){
                    onAdvertisementSucceeded()
                } else {
                    onAdvertisementFailed()
                }
            }
        }, _intervalMillis)
    }

    // Callback Implementation, just pass to own Listeners
    override fun onAdvertisementSetStart(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.map {
            try {
                it.onAdvertisementSetStart(advertisementSet)
            } catch (e:Exception){
                Log.e(_logTag, "Error in: onAdvertisementSetStart ${e.message}")
            }
        }
    }

    override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.map {
            try {
                it.onAdvertisementSetStop(advertisementSet)
            } catch (e:Exception){
                Log.e(_logTag, "Error in: onAdvertisementSetStop ${e.message}")
            }
        }

        if (!_advertisementService.isLegacyService()) {
            advertiseNextAdvertisementSet()
        }
    }

    override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
        runLocalCallback(true)
        _advertisementServiceCallbacks.map {
            try {
                it.onAdvertisementSetSucceeded(advertisementSet)
            } catch (e:Exception){
                Log.e(_logTag, "Error in: onAdvertisementSetSucceeded ${e.message}")
            }
        }
    }

    override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
        runLocalCallback(false)
        _advertisementServiceCallbacks.map {
            try {
                it.onAdvertisementSetFailed(advertisementSet, advertisementError)
            } catch (e:Exception){
                Log.e(_logTag, "Error in: onAdvertisementSetFailed ${e.message}")
            }
        }
    }
}