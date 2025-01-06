package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget

class AdvertisementViewModel : ViewModel() {
    val isAdvertising = MutableLiveData<Boolean>(false)

    val target = MutableLiveData<AdvertisementTarget>(
        AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED
    )

    val advertisementSetCollectionTitle = MutableLiveData<String>("-")
    val advertisementSetCollectionSubTitle = MutableLiveData<String>("-")
    val advertisementSetCollectionHint = MutableLiveData<String>("-")

    val advertisementSetTitle = MutableLiveData<String>("-")
    val advertisementSetSubTitle = MutableLiveData<String>("-")

    val advertisementQueueMode = MutableLiveData<AdvertisementQueueMode>(
        AdvertisementQueueMode.ADVERTISEMENT_QUEUE_MODE_RANDOM
    )

}
