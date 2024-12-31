package de.simon.dankelmann.bluetoothlespam.ui.start

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StartViewModel : ViewModel() {

    val isSeeding = MutableLiveData<Boolean>(false)

    val appVersion = MutableLiveData<String>("0.0.0")
    val androidVersion = MutableLiveData<String>(android.os.Build.VERSION.RELEASE)
    val sdkVersion = MutableLiveData<String>(android.os.Build.VERSION.SDK_INT.toString())

    val bluetoothSupport = MutableLiveData<String>("-")

    val allPermissionsGranted = MutableLiveData<Boolean>(false)

    val bluetoothAdapterIsReady = MutableLiveData<Boolean>(false)

    val advertisementServiceIsReady = MutableLiveData<Boolean>(false)

    val databaseIsReady = MutableLiveData<Boolean>(false)

    val missingRequirements = MutableLiveData<MutableList<String>>(mutableListOf())

}
