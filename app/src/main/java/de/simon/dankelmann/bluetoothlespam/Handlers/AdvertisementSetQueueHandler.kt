package de.simon.dankelmann.bluetoothlespam.Handlers

import android.os.Handler
import android.os.Looper
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementError
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IAdvertisementService
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

class AdvertisementSetQueueHandler :IAdvertisementServiceCallback {

    // private
    private var _logTag = "AdvertisementSetQueuHandler"
    private var _advertisementService:IAdvertisementService? = null
    private var _advertisementSetCollections:MutableList<MutableList<AdvertisementSet>> = mutableListOf()
    private var _interval:Long = 1000
    private var _advertisementServiceCallbacks:MutableList<IAdvertisementServiceCallback> = mutableListOf()
    private var _active = false

    init{
        _advertisementService = AppContext.getAdvertisementService()
        if(_advertisementService != null){
            _advertisementService!!.addAdvertisementServiceCallback(this)
        }
    }

    fun setTxPowerLevel(txPowerLevel:Int){
        if(_advertisementService != null){
            _advertisementService!!.setTxPowerLevel(txPowerLevel)
        }
    }

    // Add / Remove AdvertisementSetCollections
    fun clearAdvertisementSetCollection(){
        _advertisementSetCollections.clear()
    }
    fun addAdvertisementSetCollection(advertisementSetCollection: List<AdvertisementSet>){
        var mutableAdvertisementSetCollection:MutableList<AdvertisementSet> = advertisementSetCollection.toMutableList()
        if(!_advertisementSetCollections.contains(mutableAdvertisementSetCollection)){
            _advertisementSetCollections.add(mutableAdvertisementSetCollection)
        }
    }

    fun removeAdvertisementSetCollection(advertisementSetCollection: List<AdvertisementSet>){
        var mutableAdvertisementSetCollection:MutableList<AdvertisementSet> = advertisementSetCollection.toMutableList()
        if(_advertisementSetCollections.contains(mutableAdvertisementSetCollection)){
            _advertisementSetCollections.remove(mutableAdvertisementSetCollection)
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
        if(_active && _advertisementSetCollections.isNotEmpty()){
            val nextAdvertisementSetCollection = _advertisementSetCollections.random()
            val nextAdvertisementSet = nextAdvertisementSetCollection.random()

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