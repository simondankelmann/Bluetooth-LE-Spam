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
        if(_advertisementSetCollection.advertisementSetLists[advertisementSetListIndex] != null){
            if(_advertisementSetCollection.advertisementSetLists[advertisementSetListIndex].advertisementSets[advertisementSetIndex] != null){
                _currentAdvertisementSetListIndex = advertisementSetListIndex
                _currentAdvertisementSetIndex = advertisementSetIndex
                _currentAdvertisementSet = _advertisementSetCollection.advertisementSetLists[advertisementSetListIndex].advertisementSets[advertisementSetIndex]
            }
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

    fun activate(context: Context) {
        if (_active) {
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
            val preparedSet = prepareAdvertisementSet(nextSet)
            _advertisementService.startAdvertisement(preparedSet)
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

    private fun selectNextAdvertisementSet() {
        var nextAdvertisementSet: AdvertisementSet? = _currentAdvertisementSet
        var nextAdvertisementSetListIndex = _currentAdvertisementSetListIndex
        var nextAdvertisementSetIndex = _currentAdvertisementSetIndex

        when(_advertisementQueueMode){
            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_SINGLE -> {
                // If no AdvertisementSet is selected, select the first set in the first list
                if(_currentAdvertisementSet == null){
                    if(_advertisementSetCollection.advertisementSetLists.isNotEmpty()){
                        val firstList = _advertisementSetCollection.advertisementSetLists.first()
                        if(firstList.advertisementSets.isNotEmpty()){
                            nextAdvertisementSetListIndex = 0
                            nextAdvertisementSetIndex = 0
                            nextAdvertisementSet = firstList.advertisementSets.first()
                        }
                    }
                }
            }

            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR -> {
                // If no AdvertisementSet is selected, select the first set in the first list
                if(_currentAdvertisementSet == null){
                    if(_advertisementSetCollection.advertisementSetLists.isNotEmpty()){
                        val firstList = _advertisementSetCollection.advertisementSetLists.first()
                        if(firstList.advertisementSets.isNotEmpty()){
                            nextAdvertisementSetListIndex = 0
                            nextAdvertisementSetIndex = 0
                            nextAdvertisementSet = firstList.advertisementSets.first()
                        }
                    }
                } else {
                    var selectedList = _advertisementSetCollection.advertisementSetLists[_currentAdvertisementSetListIndex]
                    Log.d(_logTag, "List: ${selectedList.title}, SETS: ${selectedList.advertisementSets.count()}, CurrentIndex: ${_currentAdvertisementSetIndex}")
                    if(_currentAdvertisementSetIndex >= (selectedList.advertisementSets.count() - 1)){
                        // SET ADVERTISEMENT SET INDEX TO 0
                        nextAdvertisementSetIndex = 0

                        // SELECT NEXT LIST
                        if(_currentAdvertisementSetListIndex >= (_advertisementSetCollection.advertisementSetLists.count() - 1)){
                            nextAdvertisementSetListIndex = 0
                        } else {
                            nextAdvertisementSetListIndex++
                        }

                        selectedList = _advertisementSetCollection.advertisementSetLists[nextAdvertisementSetListIndex]

                        // SET THE ITEM
                        nextAdvertisementSet = selectedList.advertisementSets[nextAdvertisementSetIndex]
                    } else {
                        nextAdvertisementSetIndex++
                        nextAdvertisementSet = selectedList.advertisementSets[nextAdvertisementSetIndex]
                    }

                }
            }

            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LIST -> {
                // If no AdvertisementSet is selected, select the first set in the first list
                if(_currentAdvertisementSet == null){
                    if(_advertisementSetCollection.advertisementSetLists.isNotEmpty()){
                        val firstList = _advertisementSetCollection.advertisementSetLists.first()
                        if(firstList.advertisementSets.isNotEmpty()){
                            nextAdvertisementSetListIndex = 0
                            nextAdvertisementSetIndex = 0
                            nextAdvertisementSet = firstList.advertisementSets.first()
                        }
                    }
                } else {
                    var selectedList = _advertisementSetCollection.advertisementSetLists[_currentAdvertisementSetListIndex]
                    Log.d(_logTag, "List: ${selectedList.title}, SETS: ${selectedList.advertisementSets.count()}, CurrentIndex: ${_currentAdvertisementSetIndex}")
                    if(_currentAdvertisementSetIndex >= (selectedList.advertisementSets.count() - 1)){
                        // SET ADVERTISEMENT SET INDEX TO 0
                        nextAdvertisementSetIndex = 0

                        selectedList = _advertisementSetCollection.advertisementSetLists[nextAdvertisementSetListIndex]

                        // SET THE ITEM
                        nextAdvertisementSetListIndex = _currentAdvertisementSetListIndex
                        nextAdvertisementSet = selectedList.advertisementSets[nextAdvertisementSetIndex]
                    } else {
                        nextAdvertisementSetIndex++
                        nextAdvertisementSet = selectedList.advertisementSets[nextAdvertisementSetIndex]
                    }

                }
            }

            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM -> {
                nextAdvertisementSetListIndex = Random.nextInt(_advertisementSetCollection.advertisementSetLists.size);
                val nextAdvertisementSetList = _advertisementSetCollection.advertisementSetLists.get(nextAdvertisementSetListIndex)
                nextAdvertisementSetIndex = Random.nextInt(nextAdvertisementSetList.advertisementSets.size)
                nextAdvertisementSet = nextAdvertisementSetList.advertisementSets[nextAdvertisementSetIndex]
            }
        }

        _currentAdvertisementSet = nextAdvertisementSet
        _currentAdvertisementSetListIndex = nextAdvertisementSetListIndex
        _currentAdvertisementSetIndex = nextAdvertisementSetIndex
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