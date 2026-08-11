package de.simon.dankelmann.bluetoothlespam.ui.spamDetector

import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanForegroundService

private const val LOG_TAG = "SpamDetectorRoute"

/** Route entry point (plan §6 step 4) — owns the business logic `SpamDetectorFragment` used to own. */
@Composable
fun SpamDetectorRoute() {
    val context = LocalContext.current
    val viewModel: SpamDetectorViewModel = viewModel()
    val flipperDevices = remember { mutableStateListOf<FlipperDeviceScanResult>() }
    val spamPackages = remember { mutableStateListOf<SpamPackageScanResult>() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val callback = remember {
        object : IBluetoothLeScanCallback {
            override fun onScanResult(scanResult: ScanResult) {
                // Nothing to do yet
            }

            override fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown: Boolean) {
                // Nothing to do yet
            }

            override fun onFlipperListUpdated() {
                updateFlipperDevicesListView(context, flipperDevices)
            }

            override fun onSpamResultPackageDetected(spamPackageScanResult: SpamPackageScanResult, alreadyKnown: Boolean) {
                // Nothing to do yet
            }

            override fun onSpamResultPackageListUpdated() {
                updateSpamPackageListView(context, spamPackages)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val scanService = (context.applicationContext as BleSpamApplication).scanService
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    scanService.addBluetoothLeScanServiceCallback(callback)
                    viewModel.isDetecting.value = scanService.isScanning()
                    updateFlipperDevicesListView(context, flipperDevices)
                    updateSpamPackageListView(context, spamPackages)
                }
                Lifecycle.Event.ON_PAUSE -> scanService.removeBluetoothLeScanServiceCallback(callback)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isDetecting by viewModel.isDetecting.observeAsState(false)

    SpamDetectorScreen(
        isDetecting = isDetecting == true,
        flipperDevices = flipperDevices,
        spamPackages = spamPackages,
        onToggleClicked = { onToggleButtonClicked(context, viewModel) },
    )
}

private fun updateFlipperDevicesListView(context: Context, flipperDevices: SnapshotStateList<FlipperDeviceScanResult>) {
    val scanService = (context.applicationContext as BleSpamApplication).scanService
    scanService.getFlipperDevicesList().forEach { newFlipperDevice ->
        val oldIndex = flipperDevices.indexOfFirst { it.address == newFlipperDevice.address }
        if (oldIndex != -1) {
            flipperDevices[oldIndex] = newFlipperDevice
        } else {
            flipperDevices.add(newFlipperDevice)
        }
    }
}

private fun updateSpamPackageListView(context: Context, spamPackages: SnapshotStateList<SpamPackageScanResult>) {
    val scanService = (context.applicationContext as BleSpamApplication).scanService
    scanService.getSpamPackageScanResultList().forEach { newSpamPackage ->
        val oldIndex = spamPackages.indexOfFirst { it.address == newSpamPackage.address }
        if (oldIndex != -1) {
            spamPackages[oldIndex] = newSpamPackage
        } else {
            spamPackages.add(newSpamPackage)
        }
    }
}

private fun onToggleButtonClicked(context: Context, viewModel: SpamDetectorViewModel) {
    if (viewModel.isDetecting.value == true) {
        BluetoothLeScanForegroundService.stopService(context)
        Log.d(LOG_TAG, "Should Stop")
        viewModel.isDetecting.value = false
    } else {
        BluetoothLeScanForegroundService.startService(context)
        viewModel.isDetecting.value = true
    }
}
