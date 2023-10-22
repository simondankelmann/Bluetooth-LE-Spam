package de.simon.dankelmann.bluetoothlespam.Services

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet

class AdvertismentLoopService {
    private var _logTag = "AdvertismentLoopService"
    private var _advertising = false
    private var _bluetoothLeAdvertisementService:BluetoothLeAdvertisementService = BluetoothLeAdvertisementService(AppContext.getContext().bluetoothAdapter()!!)
    private var _currentIndex = 0
    private var _advertisementSets:MutableList<AdvertisementSet> = mutableListOf()


    val timer = object: CountDownTimer(10000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            // do something
            advertiseNextPackage()
        }
        override fun onFinish() {
            // do something
            Log.d(_logTag, "Timer finished")
            start()
        }
    }


    fun addAdvertisementSet(advertisementSet: AdvertisementSet){
        _advertisementSets.add(advertisementSet)
    }

    fun startAdvertising(){
        _advertising = true
        _currentIndex = 0
        timer.start()
    }

    fun stopAdvertising(){
        _advertising = false
        timer.cancel()
    }

    fun advertiseNextPackage(){
        Log.d(_logTag, "Advertising the next Package")

        if(_advertising && _advertisementSets.count() > 0){
            // stop advertising the current package
            val currentAdvertisementSet = _advertisementSets.get(_currentIndex)
            _bluetoothLeAdvertisementService.stopAdvertising(currentAdvertisementSet)

            val maxIndex = _advertisementSets.count() - 1
            if(_currentIndex < maxIndex){
                _currentIndex++

            }

            //start the new one
            val newAdvertisementSet = _advertisementSets.get(_currentIndex)
            _bluetoothLeAdvertisementService.startAdvertising(currentAdvertisementSet)

            // exectute again with delay
            /*
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post(object : Runnable {
                override fun run() {
                    advertiseNextPackage()
                    mainHandler.postDelayed(this, 1000)
                }
            })*/
        }
    }

}