package de.simon.dankelmann.bluetoothlespam.Services

import android.os.CountDownTimer
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

class AdvertisementLoopService (bluetoothLeAdvertisementService:BluetoothLeAdvertisementService){
    private var _logTag = "AdvertisementLoopService"
    private var _advertising = false
    private var _bluetoothLeAdvertisementService:BluetoothLeAdvertisementService = bluetoothLeAdvertisementService
    private var _currentIndex = 0
    private var _advertisementSets:MutableList<AdvertisementSet> = mutableListOf()
    private var _bleAdvertisementServiceCallback:MutableList<IBleAdvertisementServiceCallback> = mutableListOf()

    private val _maxAdvertisers = 1
    private var _currentAdvertisers:MutableList<AdvertisementSet> = mutableListOf()

    val timer = object: CountDownTimer(10000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            advertiseNextPackage(true)
            //batchIt()
        }
        override fun onFinish() {
            // do something
            Log.d(_logTag, "Timer finished, restarting")
            start()
        }
    }

    fun addAdvertisementSet(advertisementSet: AdvertisementSet){
        _advertisementSets.add(advertisementSet)
    }

    fun startAdvertising(){
        val hardwareCheck = _bluetoothLeAdvertisementService.checkHardware()
        Log.d(_logTag, "Hardware Check returns: ${hardwareCheck}");
        _advertising = true
        _currentIndex = 0
        timer.start()
        _bleAdvertisementServiceCallback.map {
            it.onAdvertisementStarted()
        }
    }

    fun stopAdvertising(){
        _advertising = false
        _currentIndex = 0
        timer.cancel()
        stopAllAdvertisers()
        _bleAdvertisementServiceCallback.map {
            it.onAdvertisementStopped()
        }
    }

    fun stopAllAdvertisers(){
        _advertisementSets.map{
            _bluetoothLeAdvertisementService.stopAdvertisingSet(it)
        }
    }

    fun cleanupAdvertisers(){
        if(_currentAdvertisers.count() > _maxAdvertisers){
            // remove the first advertiser
            var advertiserToRemove = _currentAdvertisers[0]
            _bluetoothLeAdvertisementService.stopAdvertisingSet(advertiserToRemove)
            _currentAdvertisers.removeAt(0)
            Log.d(_logTag, "Removed advertiser for: " + advertiserToRemove.deviceName)
        }
    }

    fun batchIt(){
        stopAllAdvertisers()
        for (i in 0.._maxAdvertisers){
            advertiseNextPackage(false)
        }
    }


    fun advertiseNextPackage(clean: Boolean = true){
        
        if(_advertising && _advertisementSets.count() > 0){

            // clean if there are already too many advertisers
            if(clean){
                cleanupAdvertisers()
            }

            val nextAdvertisementSet = _advertisementSets[_currentIndex]

            _currentAdvertisers.add(nextAdvertisementSet)
            _bluetoothLeAdvertisementService.startAdvertising(nextAdvertisementSet)
            // _bluetoothLeAdvertisementService.startAdvertisingSet(nextAdvertisementSet)


            Log.d(_logTag, "Added advertiser for: " + nextAdvertisementSet.deviceName);

            val maxIndex = _advertisementSets.count() - 1

            if(_currentIndex < maxIndex){
                // go the next item
                _currentIndex++
            } else {
                // go back to the start
                _currentIndex = 0
            }

        }
    }

    fun addBleAdvertisementServiceCallback(callback: IBleAdvertisementServiceCallback){
        _bleAdvertisementServiceCallback.add(callback)
    }

}