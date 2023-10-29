package de.simon.dankelmann.bluetoothlespam.Services

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.os.CountDownTimer
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import java.util.Timer
import java.util.TimerTask

class AdvertisementLoopService (bluetoothLeAdvertisementService:BluetoothLeAdvertisementService) {
    private var _logTag = "AdvertisementLoopService"
    var advertising = false
    private var _bluetoothLeAdvertisementService:BluetoothLeAdvertisementService = bluetoothLeAdvertisementService
    private var _currentIndex = 0
    private var _advertisementSets:MutableList<AdvertisementSet> = mutableListOf()
    private var _bleAdvertisementServiceCallback:MutableList<IBleAdvertisementServiceCallback> = mutableListOf()

    private val _maxAdvertisers = 1
    private var _currentAdvertisers:MutableList<AdvertisementSet> = mutableListOf()

    private var countdownInterval = 1000
    private var millisInFuture = 10000
    private var timer:CountDownTimer = getTimer()

    fun setIntervalSeconds(interval:Int){
        timer.cancel()

        countdownInterval = interval * 1000
        millisInFuture = countdownInterval * 10

        timer = getTimer()
        if(advertising){
            timer.start()
        }
    }

    private fun getTimer():CountDownTimer{
        return object: CountDownTimer(millisInFuture.toLong(), countdownInterval.toLong()) {
            override fun onTick(millisUntilFinished: Long) {
                advertiseNextPackage(true)
                //batchIt()
            }
            override fun onFinish() {
                Log.d(_logTag, "Timer finished, restarting")
                this.start()
            }
        }
    }

    fun addAdvertisementSet(advertisementSet: AdvertisementSet){
        // overwrite with own callbacks
        var advertisementSetToAdd = advertisementSet
        advertisementSetToAdd.advertisingCallback = advertiseCallback
        advertisementSetToAdd.advertisingSetCallback = advertisingSetCallback
        _advertisementSets.add(advertisementSetToAdd)
    }

    fun startAdvertising(){
        val hardwareCheck = _bluetoothLeAdvertisementService.checkHardware()
        Log.d(_logTag, "Hardware Check returns: ${hardwareCheck}");
        advertising = true
        _currentIndex = 0


        timer.cancel()
        timer = getTimer()
        timer.start()

        //advertiseNextPackage()

        _bleAdvertisementServiceCallback.map {
            it.onAdvertisementStarted()
        }
    }

    fun stopAdvertising(){
        advertising = false
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
        if(_currentAdvertisers.count() >= _maxAdvertisers){
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
        
        if(advertising && _advertisementSets.count() > 0){

            // clean if there are already too many advertisers
            if(clean){
                cleanupAdvertisers()
            }

            //val nextAdvertisementSet = _advertisementSets[_currentIndex]
            val nextAdvertisementSet = _advertisementSets.random()

            _currentAdvertisers.add(nextAdvertisementSet)
            //_bluetoothLeAdvertisementService.startAdvertising(nextAdvertisementSet)
            _bluetoothLeAdvertisementService.startAdvertisingSet(nextAdvertisementSet)

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

    // Own Callbacks
    val advertiseCallback:AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            _bleAdvertisementServiceCallback.map{
                it.onStartFailure(errorCode)
            }
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            _bleAdvertisementServiceCallback.map{
                it.onStartSuccess(settingsInEffect)
            }
        }
    }

    val advertisingSetCallback:AdvertisingSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {
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
            }
        }
    }

}