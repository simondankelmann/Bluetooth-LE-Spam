package de.simon.dankelmann.bluetoothlespam.ui.fastPairing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FastPairingViewModel : ViewModel() {

    private val _statusText = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    public fun setStatusText(text:String){
        _statusText.postValue(text)
    }

    val isTransmitting = MutableLiveData<Boolean>().apply {
        value = false
    }
    //val isTransmitting: LiveData<Boolean> = _isTransmitting


    val statusText: LiveData<String> = _statusText
}