package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AdvertisementViewModel : ViewModel() {
    var _statusText = MutableLiveData<String>().apply {
        value = "Status"
    }
}