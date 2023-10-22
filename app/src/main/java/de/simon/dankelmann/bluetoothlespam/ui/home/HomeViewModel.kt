package de.simon.dankelmann.bluetoothlespam.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }

    public fun setText(text:String){
        _text.postValue(text)
    }

    val text: LiveData<String> = _text
}