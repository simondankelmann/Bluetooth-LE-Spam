package de.simonkelmann.bluetoothlespam.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private var _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }



    val text: LiveData<String> = _text

    public fun setText(text:String){
        _text.postValue(text)
    }
}