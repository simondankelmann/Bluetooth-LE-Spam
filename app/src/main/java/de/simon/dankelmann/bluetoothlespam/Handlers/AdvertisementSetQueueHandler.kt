package de.simon.dankelmann.bluetoothlespam.Handlers

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

class  AdvertisementSetQueueHandler :IAdvertisementServiceCallback{

    // private
    private var _logTag = "AdvertisementSetQueuHandler"
    private var _advertisementService:IAdvertisementService? = null
    private var _advertisementSetCollection:AdvertisementSetCollection = AdvertisementSetCollection()
    private var _interval:Long = 1000
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    private var _advertisementQueueHandlerCallbacks:MutableList<IAdvertisementSetQueueHandlerCallback> = mutableListOf()

    private var _active = false
    private var _advertisementQueueMode: AdvertisementQueueMode = AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR

    private var _currentAdvertisementSet: AdvertisementSet? = null
    private var _currentAdvertisementSetListIndex = 0
    private var _currentAdvertisementSetIndex = 0

    // Multi-selection: ids of the sets the user tapped. When non-empty,
    // advertising is restricted to this pool (in collection order).
    private var _selectedIds: LinkedHashSet<Int> = LinkedHashSet()


    init{
        _advertisementService = AppContext.getAdvertisementService()
        if(_advertisementService != null){
            _advertisementService!!.addAdvertisementServiceCallback(this)
        }

        setInterval(QueueHandlerHelpers.getInterval())
    }

    fun setAdvertisementQueueMode(advertisementQueueMode: AdvertisementQueueMode){
        _advertisementQueueMode = advertisementQueueMode
    }

    fun getAdvertisementQueueMode():AdvertisementQueueMode{
        return _advertisementQueueMode
    }

    fun setAdvertisementService(advertisementService: IAdvertisementService){
        _advertisementService = advertisementService
        _advertisementService!!.addAdvertisementServiceCallback(this)
    }

    fun setTxPowerLevel(txPowerLevel: TxPowerLevel){
        if(_advertisementService != null){
            _advertisementService!!.setTxPowerLevel(txPowerLevel)
        }
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

    fun getCurrentAdvertisementSet(): AdvertisementSet? {
        return _currentAdvertisementSet
    }

    fun clearCurrentAdvertisementSet() {
        _currentAdvertisementSet = null
        _currentAdvertisementSetListIndex = 0
        _currentAdvertisementSetIndex = 0
    }

    // Multi-selection (toggle). Returns the new selected state of the set.
    fun toggleSelectedAdvertisementSet(advertisementSet: AdvertisementSet): Boolean {
        val target = findSetById(advertisementSet.id) ?: advertisementSet
        return if (_selectedIds.contains(target.id)) {
            _selectedIds.remove(target.id)
            target.selected = false
            false
        } else {
            _selectedIds.add(target.id)
            target.selected = true
            true
        }
    }

    fun isSelected(advertisementSet: AdvertisementSet): Boolean {
        return _selectedIds.contains(advertisementSet.id)
    }

    // Bulk (de)selection, e.g. a whole category via long-press on its header
    fun setSetsSelected(sets: List<AdvertisementSet>, selected: Boolean) {
        sets.forEach { set ->
            val target = findSetById(set.id) ?: set
            if (selected) {
                _selectedIds.add(target.id)
                target.selected = true
            } else {
                _selectedIds.remove(target.id)
                target.selected = false
            }
        }
    }

    fun getSelectedIds(): Set<Int> {
        return _selectedIds.toSet()
    }

    fun clearSelection() {
        _selectedIds.clear()
        _advertisementSetCollection.advertisementSetLists.forEach { list ->
            list.advertisementSets.forEach { it.selected = false }
        }
    }

    fun retainSelection(presentIds: Set<Int>) {
        _selectedIds.retainAll(presentIds)
    }

    fun findSetById(id: Int): AdvertisementSet? {
        _advertisementSetCollection.advertisementSetLists.forEach { list ->
            list.advertisementSets.forEach {
                if (it.id == id) return it
            }
        }
        return null
    }

    fun getSelectedOrdered(): List<AdvertisementSet> {
        if (_selectedIds.isEmpty()) return emptyList()
        val ordered = mutableListOf<AdvertisementSet>()
        _advertisementSetCollection.advertisementSetLists.forEach { list ->
            list.advertisementSets.forEach {
                if (_selectedIds.contains(it.id)) ordered.add(it)
            }
        }
        return ordered
    }

    fun setAdvertisementSetCollection(advertisementSetCollection: AdvertisementSetCollection, clearSelection: Boolean = true){
        if(_advertisementSetCollection != advertisementSetCollection){
            _advertisementSetCollection = advertisementSetCollection
        }

        // Reset indices
        _currentAdvertisementSet= null
        _currentAdvertisementSetListIndex = 0
        _currentAdvertisementSetIndex = 0

        if(clearSelection){
            clearSelection()
        }
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

    fun setIntervalSeconds(seconds:Int){
        _interval = (seconds * 1000).toLong()
    }

    fun setInterval(milliseconds:Int){
        if(milliseconds > 0){
            _interval = milliseconds.toLong()
        }
    }

    fun activate(startService: Boolean = true){
        if(!_active){
            _active = true

            if(startService){
                AdvertisementForegroundService.startService(AppContext.getContext(), "Foreground Service is running...")
            }

            _advertisementQueueHandlerCallbacks.forEach { it ->
                try {
                    it.onQueueHandlerActivated()
                } catch (e:Exception){
                    Log.e(_logTag, "Error while executing AdvertisementQueueHandlerCallback onQueueHandlerActivated")
                }
            }

            if(_currentAdvertisementSet != null){
                handleAdvertisementSet(_currentAdvertisementSet!!)
            } else {
                advertiseNextAdvertisementSet()
            }
        }
    }

    fun deactivate(stopService: Boolean = false){
        _active = false

        if(AppContext.getAdvertisementService() != null){
            AppContext.getAdvertisementService().stopAdvertisement()
        }

        if(stopService){
            Log.d(_logTag, "Stopping Foreground Service")
            AdvertisementForegroundService.stopService(AppContext.getActivity())
        }

        _advertisementQueueHandlerCallbacks.forEach { it ->
            try {
                it.onQueueHandlerDeactivated()
            } catch (e:Exception){
                Log.e(_logTag, "Error while executing AdvertisementQueueHandlerCallback onQueueHandlerDeactivated")
            }
        }
    }

    fun advertiseNextAdvertisementSet(){
        selectNextAdvertisementSet()
        if(_currentAdvertisementSet != null){
            handleAdvertisementSet(prepareAdvertisementSet(_currentAdvertisementSet!!))
        } else {
            Log.e(_logTag, "Current Advertisement Set is null.")
        }
    }

    fun prepareAdvertisementSet(advertisementSet: AdvertisementSet):AdvertisementSet{
        when(advertisementSet.type){
            // Continuity
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_DEVICE -> return ContinuityNewDevicePopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NEW_AIRTAG -> return ContinuityNewAirtagPopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_NOT_YOUR_DEVICE -> return ContinuityNotYourDevicePopUpAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)

            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_ACTION_MODALS -> return ContinuityActionModalAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)
            AdvertisementSetType.ADVERTISEMENT_TYPE_CONTINUITY_IOS_17_CRASH -> return ContinuityIos17CrashAdvertisementSetGenerator.prepareAdvertisementSet(advertisementSet)

            else -> return advertisementSet
        }
    }

    fun selectNextAdvertisementSet(){
        // When the user selected specific sets, restrict advertising to that pool
        val pool = getSelectedOrdered()
        if(pool.isNotEmpty()){
            selectNextFromPool(pool)
            return
        }

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

    private fun selectNextFromPool(pool: List<AdvertisementSet>){
        when(_advertisementQueueMode){
            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_SINGLE -> {
                // Repeat the last tapped set, fall back to the first selected one
                val current = _currentAdvertisementSet
                if(current == null || !_selectedIds.contains(current.id)){
                    _currentAdvertisementSet = pool.first()
                }
            }

            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LINEAR,
            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_LIST -> {
                val current = _currentAdvertisementSet
                val currentIndex = pool.indexOfFirst { it.id == current?.id }
                val nextIndex = if(currentIndex == -1) 0 else (currentIndex + 1) % pool.size
                _currentAdvertisementSet = pool[nextIndex]
                updateIndicesForSet(pool[nextIndex])
            }

            AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM -> {
                val nextIndex = Random.nextInt(pool.size)
                _currentAdvertisementSet = pool[nextIndex]
                updateIndicesForSet(pool[nextIndex])
            }
        }
    }

    private fun updateIndicesForSet(advertisementSet: AdvertisementSet){
        _advertisementSetCollection.advertisementSetLists.forEachIndexed { listIndex, list ->
            list.advertisementSets.forEachIndexed { setIndex, set ->
                if(set.id == advertisementSet.id){
                    _currentAdvertisementSetListIndex = listIndex
                    _currentAdvertisementSetIndex = setIndex
                    return
                }
            }
        }
    }

    private fun handleAdvertisementSet(advertisementSet: AdvertisementSet){
        if(_active && _advertisementService != null){
            _advertisementService!!.startAdvertisement(advertisementSet)
        }
    }

    fun isActive():Boolean{
        return _active
    }

    fun onAdvertisementSucceeded(){
        if(_advertisementService != null){
            _advertisementService!!.stopAdvertisement()

            if(_advertisementService!!.isLegacyService()){
                advertiseNextAdvertisementSet()
            } else {
                // Wait for the Stop Advertising Callback
            }
        }
    }

    fun onAdvertisementFailed(){
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
        }, _interval)
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

        if(_advertisementService != null && !_advertisementService!!.isLegacyService()){
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