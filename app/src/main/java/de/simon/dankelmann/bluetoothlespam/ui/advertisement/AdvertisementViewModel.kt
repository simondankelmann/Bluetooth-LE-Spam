package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget

class AdvertisementViewModel : ViewModel() {
    var isAdvertising = MutableLiveData<Boolean>().apply {
        value = false
    }

    var target = MutableLiveData<AdvertisementTarget>().apply {
        value = AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED
    }

    var advertisementSetCollectionTitle = MutableLiveData<String>().apply {
        value = "-"
    }

    var advertisementSetCollectionSubTitle = MutableLiveData<String>().apply {
        value = "-"
    }

    var advertisementSetCollectionHint = MutableLiveData<String>().apply {
        value = "-"
    }

    var advertisementSetTitle = MutableLiveData<String>().apply {
        value = "-"
    }

    var advertisementSetSubTitle = MutableLiveData<String>().apply {
        value = "-"
    }

    var advertisementQueueMode = MutableLiveData<AdvertisementQueueMode>().apply {
        value = AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM
    }

}