package de.simon.dankelmann.bluetoothlespam.Services

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

class AdvertisementSetQueHandler(bluetoothLeAdvertisementService:BluetoothLeAdvertisementService) {
    // private
    private var _logTag = "AdvertisementSetQueHandler"
    private var _currentAdvertiesementSet:AdvertisementSet? = null
    private val _bluetoothLeAdvertisementService:BluetoothLeAdvertisementService = bluetoothLeAdvertisementService
    private var _advertisementSetCollections:MutableList<MutableList<AdvertisementSet>> = mutableListOf()
    private var _bleAdvertisementServiceCallback:MutableList<IBleAdvertisementServiceCallback> = mutableListOf()
    private var _interval:Long = 1000

    // public
    var advertising = false

    fun addAdvertisementSetCollection(advertisementSetCollection: List<AdvertisementSet>){
        var newCollection:MutableList<AdvertisementSet> = mutableListOf()

        advertisementSetCollection.map{
            var advertisementSetToAdd = it
            // overwrite with own callbacks
            advertisementSetToAdd.advertisingCallback = advertiseCallback
            advertisementSetToAdd.advertisingSetCallback = advertisingSetCallback
            newCollection.add(advertisementSetToAdd)
        }

        _advertisementSetCollections.add(newCollection)
    }

    fun addBleAdvertisementServiceCallback(callback: IBleAdvertisementServiceCallback){
        _bleAdvertisementServiceCallback.add(callback)
    }

    fun setIntervalSeconds(seconds:Int){
        _interval = (seconds * 1000).toLong()
    }

    fun startAdvertising(){
        advertising = true

        advertiseNextAdvertisementSet()

        _bleAdvertisementServiceCallback.map {
            it.onAdvertisementStarted()
        }
    }

    fun stopAdvertising(){
        advertising = false

        _bleAdvertisementServiceCallback.map {
            it.onAdvertisementStopped()
        }
    }

    fun advertiseNextAdvertisementSet(){
        if(advertising && _advertisementSetCollections.isNotEmpty()){
            val nextAdvertisementSetCollection = _advertisementSetCollections.random()
            val nextAdvertisementSet = nextAdvertisementSetCollection.random()

            _currentAdvertiesementSet = nextAdvertisementSet
            _bluetoothLeAdvertisementService.advertiseSet(nextAdvertisementSet)
        }
    }

    fun advertisementSucceeded(){
        stopCurrentAdvertisementSet()
        advertiseNextAdvertisementSet()
    }

    fun advertisementFailed(){
        Log.d(_logTag, "Trying it again")
        advertisementSucceeded()
    }

    fun stopCurrentAdvertisementSet(){
        if(_currentAdvertiesementSet != null){
            _bluetoothLeAdvertisementService.stopAdvertiseSet(_currentAdvertiesementSet!!)
            _currentAdvertiesementSet = null
        }
    }

    private fun runLocalCallback(success:Boolean){
        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
            override fun run() {
                if(success){
                    advertisementSucceeded()
                } else {
                    advertisementFailed()
                }
            }
        }, _interval)
    }

    // Callback implementation
    // - -- --- ADVERTISECALLBACK --- -- - //
    val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            runLocalCallback(false)
            _bleAdvertisementServiceCallback.map {
                it.onStartFailure(errorCode)
            }
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            runLocalCallback(true)
            _bleAdvertisementServiceCallback.map {
                it.onStartSuccess(settingsInEffect)
            }
        }
    }

    // - -- --- ADVERTISINGSETCALLBACK --- -- - //
    val advertisingSetCallback: AdvertisingSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
            if(status == AdvertisingSetCallback.ADVERTISE_SUCCESS){
                // STOP ADVERTISING DELAYED, WAIT FOR STOP-SUCCESS, THEN ADVERTISE THE NEXT
                Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                    override fun run() {
                        stopCurrentAdvertisementSet()
                    }
                }, _interval)

            } else{
                Log.d(_logTag, "Failed: ${status}")
                runLocalCallback(false)
            }

            _bleAdvertisementServiceCallback.map{
                it.onAdvertisingSetStarted(advertisingSet, txPower, status)
            }
        }

        override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
            _bleAdvertisementServiceCallback.map{
                it.onAdvertisingDataSet(advertisingSet, status)
            }
        }

        override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
            _bleAdvertisementServiceCallback.map{
                it.onScanResponseDataSet(advertisingSet, status)
            }
        }

        override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
            _bleAdvertisementServiceCallback.map{
                it.onAdvertisingSetStopped(advertisingSet)
                Log.d(_logTag, "ADVERTISING SET WAS STOPPED")
                runLocalCallback(true)
            }
        }
    }

}