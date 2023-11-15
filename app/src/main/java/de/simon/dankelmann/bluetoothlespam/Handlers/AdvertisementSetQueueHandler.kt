package de.simon.dankelmann.bluetoothlespam.Handlers

import android.os.Handler
import android.os.Looper
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList

class  AdvertisementSetQueueHandler :IAdvertisementServiceCallback {

    // private
    private var _logTag = "AdvertisementSetQueuHandler"
    private var _advertisementService:IAdvertisementService? = null
    private var _advertisementSetCollection:AdvertisementSetCollection = AdvertisementSetCollection()
    private var _interval:Long = 1000
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    private var _active = false

    init{
        _advertisementService = AppContext.getAdvertisementService()
        if(_advertisementService != null){
            _advertisementService!!.addAdvertisementServiceCallback(this)
        }
    }

    fun setAdvertisementService(advertisementService: IAdvertisementService){
        _advertisementService = advertisementService
        _advertisementService!!.addAdvertisementServiceCallback(this)
    }

    fun setTxPowerLevel(txPowerLevel:Int){
        if(_advertisementService != null){
            _advertisementService!!.setTxPowerLevel(txPowerLevel)
        }
    }

    fun setAdvertisementSetCollection(advertisementSetCollection: AdvertisementSetCollection){
        _advertisementSetCollection = advertisementSetCollection
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

    fun setIntervalSeconds(seconds:Int){
        _interval = (seconds * 1000).toLong()
    }

    fun activate(){
        _active = true
        handleNextAdvertisementSet()
    }

    fun deactivate(){
        _active = false
        if(_advertisementService != null){
            _advertisementService!!.stopAdvertisement()
        }
    }

    private fun handleNextAdvertisementSet(){
        if(_active && _advertisementSetCollection.advertisementSetLists.isNotEmpty()){
            val nextAdvertisementSetList = _advertisementSetCollection.advertisementSetLists.random()
            val nextAdvertisementSet = nextAdvertisementSetList.advertisementSets.random()

            if(_advertisementService != null){
                _advertisementService!!.startAdvertisement(nextAdvertisementSet)
            }
        }
    }

    fun isActive():Boolean{
        return _active
    }

    fun onAdvertisementSucceeded(){
        if(_advertisementService != null){
            _advertisementService!!.stopAdvertisement()

            if(_advertisementService!!.isLegacyService()){
                handleNextAdvertisementSet()
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
            it.onAdvertisementSetStart(advertisementSet)
        }
    }

    override fun onAdvertisementSetStop(advertisementSet: AdvertisementSet?) {
        _advertisementServiceCallbacks.map {
            it.onAdvertisementSetStop(advertisementSet)
        }

        if(_advertisementService != null && !_advertisementService!!.isLegacyService()){
            handleNextAdvertisementSet()
        }
    }

    override fun onAdvertisementSetSucceeded(advertisementSet: AdvertisementSet?) {
        runLocalCallback(true)
        _advertisementServiceCallbacks.map {
            it.onAdvertisementSetSucceeded(advertisementSet)
        }
    }

    override fun onAdvertisementSetFailed(advertisementSet: AdvertisementSet?, advertisementError: AdvertisementError) {
        runLocalCallback(false)
        _advertisementServiceCallbacks.map {
            it.onAdvertisementSetFailed(advertisementSet, advertisementError)
        }
    }
}