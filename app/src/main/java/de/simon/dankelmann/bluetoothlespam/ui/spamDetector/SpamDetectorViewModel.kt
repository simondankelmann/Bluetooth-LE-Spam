package de.simon.dankelmann.bluetoothlespam.ui.spamDetector

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SpamDetectorViewModel : ViewModel() {
    var isDetecting = MutableLiveData<Boolean>().apply {
        value = false
    }
}