package de.simon.dankelmann.bluetoothlespam.ui.start

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StartViewModel : ViewModel() {

    val isSeeding = MutableLiveData<Boolean>(false)

    val appVersion = MutableLiveData<String>("0.0.0")
    val androidVersion = MutableLiveData<String>(android.os.Build.VERSION.RELEASE)
    val sdkVersion = MutableLiveData<String>(android.os.Build.VERSION.SDK_INT.toString())

    val bluetoothSupport = MutableLiveData<String>("-")

    // null = not checked yet (rendered as the loading state on the Requirements card)
    val allPermissionsGranted = MutableLiveData<Boolean?>(null)

    val bluetoothAdapterIsReady = MutableLiveData<Boolean?>(null)

    val databaseIsReady = MutableLiveData<Boolean?>(null)

    val missingRequirements = MutableLiveData<MutableList<String>>(mutableListOf())

}
