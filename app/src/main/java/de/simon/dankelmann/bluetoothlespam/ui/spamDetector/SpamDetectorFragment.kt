package de.simon.dankelmann.bluetoothlespam.ui.spamDetector

import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import de.simon.dankelmann.bluetoothlespam.Adapters.FlipperDeviceScanResultListViewAdapter
import de.simon.dankelmann.bluetoothlespam.Adapters.SpamPackageScanResultListViewAdapter
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanForegroundService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentSpamDetectorBinding

class SpamDetectorFragment : IBluetoothLeScanCallback, Fragment() {

    private val _logTag = "SpamDetectorFragment"

    private var _viewModel: SpamDetectorViewModel? = null
    private val viewModel get() = _viewModel!!

    private var _binding: FragmentSpamDetectorBinding? = null
    private val binding get() = _binding!!

    private lateinit var _flipperDevicesRecyclerView: RecyclerView
    private lateinit var _flipperDevicesListViewAdapter: FlipperDeviceScanResultListViewAdapter

    private lateinit var _spamPackageRecyclerView: RecyclerView
    private lateinit var _spamPackageListViewAdapter: SpamPackageScanResultListViewAdapter

    override fun onResume() {
        super.onResume()
        AppContext.getBluetoothLeScanService().addBluetoothLeScanServiceCallback(this)
        // SYNC THE LISTS
        syncWithScanServices()
    }

    override fun onPause() {
        super.onPause()
        AppContext.getBluetoothLeScanService().removeBluetoothLeScanServiceCallback(this)
    }

    private fun syncWithScanServices() {
        viewModel.isDetecting.postValue(AppContext.getBluetoothLeScanService().isScanning())
        updateFlipperDevicesListView()
        updateSpamPackageListView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewModel = ViewModelProvider(this)[SpamDetectorViewModel::class.java]
        _binding = FragmentSpamDetectorBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _flipperDevicesRecyclerView = binding.spamDetectionFlipperDevicesList
        _spamPackageRecyclerView = binding.spamDetectionSpamPackageList

        setupUi()
        setupFlipperDevicesListView()
        setupSpamPackagesListView(root.context)

        AppContext.getBluetoothLeScanService().addBluetoothLeScanServiceCallback(this)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupUi() {
        // Views
        val toggleButton = binding.spamDetectorToggleButton
        val detectionAnimation = binding.spamDetectionAnimation

        // Listeners
        toggleButton.setOnClickListener {
            onToggleButtonClicked()
        }

        // Observers
        viewModel.isDetecting.observe(viewLifecycleOwner) { isDetecting ->
            if (isDetecting) {
                toggleButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.pause, AppContext.getContext().theme
                    )
                )
                detectionAnimation.playAnimation()
            } else {
                toggleButton.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.play_arrow, AppContext.getContext().theme
                    )
                )
                detectionAnimation.cancelAnimation()
                detectionAnimation.frame = 0
            }
        }
    }

    fun setupFlipperDevicesListView() {
        _flipperDevicesListViewAdapter = FlipperDeviceScanResultListViewAdapter(
            AppContext.getBluetoothLeScanService().getFlipperDevicesList()
        )
        _flipperDevicesRecyclerView.adapter = _flipperDevicesListViewAdapter
    }

    fun setupSpamPackagesListView(context: Context) {
        _spamPackageListViewAdapter = SpamPackageScanResultListViewAdapter(
            AppContext.getBluetoothLeScanService().getSpamPackageScanResultList(), context
        )
        _spamPackageRecyclerView.adapter = _spamPackageListViewAdapter
    }

    fun updateFlipperDevicesListView() {
        if (_flipperDevicesListViewAdapter != null) {
            var newItems = AppContext.getBluetoothLeScanService().getFlipperDevicesList()
            newItems.forEach { newFlipperDevice ->
                var oldFlipperListIndex = -1
                _flipperDevicesListViewAdapter.mList.forEachIndexed { index, oldFlipperDevice ->
                    if (oldFlipperDevice.address == newFlipperDevice.address) {
                        oldFlipperListIndex = index
                    }
                }

                if (oldFlipperListIndex != -1) {
                    // Update
                    _flipperDevicesListViewAdapter.mList[oldFlipperListIndex] = newFlipperDevice
                    //Log.d(_logTag, "Updated existing Item")
                } else {
                    // Add
                    _flipperDevicesListViewAdapter.mList.add(newFlipperDevice)
                    //Log.d(_logTag, "Created existing Item")
                }
            }

            _flipperDevicesListViewAdapter.notifyDataSetChanged()
        }
    }

    fun updateSpamPackageListView() {
        if (_spamPackageListViewAdapter != null) {
            var newItems = AppContext.getBluetoothLeScanService().getSpamPackageScanResultList()
            newItems.forEach { newSpamPackage ->
                var oldListIndex = -1

                _spamPackageListViewAdapter.mList.forEachIndexed { index, oldSpamPackage ->
                    if (oldSpamPackage.address == newSpamPackage.address) {
                        oldListIndex = index
                    }
                }

                if (oldListIndex != -1) {
                    // Update
                    _spamPackageListViewAdapter.mList[oldListIndex] = newSpamPackage
                } else {
                    // Add
                    _spamPackageListViewAdapter.mList.add(newSpamPackage)
                }
            }

            _spamPackageListViewAdapter.notifyDataSetChanged()
        }
    }

    fun onToggleButtonClicked() {
        if (viewModel.isDetecting.value == true) {
            BluetoothLeScanForegroundService.stopService(AppContext.getContext())
            //AppContext.getBluetoothLeScanService().stopScanning()
            Log.d(_logTag, "Should Stop")
            viewModel.isDetecting.postValue(false)
        } else {
            BluetoothLeScanForegroundService.startService(
                AppContext.getContext(),
                "Bluetooth LE Scan Foreground Service started..."
            )
            //AppContext.getBluetoothLeScanService().startScanning()
            viewModel.isDetecting.postValue(true)
        }
    }

    // BluetoothLeScanResult Callback Implementation
    override fun onScanResult(scanResult: ScanResult) {
        // Nothing to do yet
    }

    override fun onFlipperDeviceDetected(
        flipperDeviceScanResult: FlipperDeviceScanResult,
        alreadyKnown: Boolean
    ) {
        // Nothing to do yet
        //updateFlipperDevicesListView()
    }

    override fun onFlipperListUpdated() {
        updateFlipperDevicesListView()
    }

    override fun onSpamResultPackageDetected(
        spamPackageScanResult: SpamPackageScanResult,
        alreadyKnown: Boolean
    ) {
        // Nothing to do yet
        //updateSpamPackageListView()
    }

    override fun onSpamResultPackageListUpdated() {
        updateSpamPackageListView()
    }

}