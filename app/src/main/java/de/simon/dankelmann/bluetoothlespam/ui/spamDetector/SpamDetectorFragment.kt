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
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Services.IBluetoothLeScanService
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

        val scanService = (requireContext().applicationContext as BleSpamApplication).scanService
        scanService.addBluetoothLeScanServiceCallback(this)

        syncWithScanServices(requireContext())
    }

    override fun onPause() {
        super.onPause()

        val scanService = (requireContext().applicationContext as BleSpamApplication).scanService
        scanService.removeBluetoothLeScanServiceCallback(this)
    }

    private fun syncWithScanServices(context: Context) {
        val scanService = (context.applicationContext as BleSpamApplication).scanService
        viewModel.isDetecting.postValue(scanService.isScanning())
        updateFlipperDevicesListView(context)
        updateSpamPackageListView(context)
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

        setupUi(root.context)
        setupFlipperDevicesListView(root.context)
        setupSpamPackagesListView(root.context)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupUi(context: Context) {
        // Views
        val toggleButton = binding.spamDetectorToggleButton
        val detectionAnimation = binding.spamDetectionAnimation

        // Listeners
        toggleButton.setOnClickListener {
            onToggleButtonClicked(context)
        }

        // Observers
        viewModel.isDetecting.observe(viewLifecycleOwner) { isDetecting ->
            if (isDetecting) {
                toggleButton.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.pause, context.theme)
                )
                detectionAnimation.playAnimation()
            } else {
                toggleButton.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.play_arrow, context.theme)
                )
                detectionAnimation.cancelAnimation()
                detectionAnimation.frame = 0
            }
        }
    }

    fun setupFlipperDevicesListView(context: Context) {
        val scanService = (context.applicationContext as BleSpamApplication).scanService
        _flipperDevicesListViewAdapter = FlipperDeviceScanResultListViewAdapter(
            scanService.getFlipperDevicesList()
        )
        _flipperDevicesRecyclerView.adapter = _flipperDevicesListViewAdapter
    }

    fun setupSpamPackagesListView(context: Context) {
        val scanService = (context.applicationContext as BleSpamApplication).scanService
        _spamPackageListViewAdapter = SpamPackageScanResultListViewAdapter(
            scanService.getSpamPackageScanResultList(), context
        )
        _spamPackageRecyclerView.adapter = _spamPackageListViewAdapter
    }

    fun updateFlipperDevicesListView(context: Context) {
        val scanService = (context.applicationContext as BleSpamApplication).scanService
        if (_flipperDevicesListViewAdapter != null) {
            var newItems = scanService.getFlipperDevicesList()
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

    fun updateSpamPackageListView(context: Context) {
        val scanService = (context.applicationContext as BleSpamApplication).scanService
        if (_spamPackageListViewAdapter != null) {
            var newItems = scanService.getSpamPackageScanResultList()
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

    fun onToggleButtonClicked(context: Context) {
        if (viewModel.isDetecting.value == true) {
            BluetoothLeScanForegroundService.stopService(context)
            //AppContext.getBluetoothLeScanService().stopScanning()
            Log.d(_logTag, "Should Stop")
            viewModel.isDetecting.postValue(false)
        } else {
            BluetoothLeScanForegroundService.startService(context)
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
        // The fragment could be in the background, then the context is invalid
        context?.let {
            updateFlipperDevicesListView(it)
        }
    }

    override fun onSpamResultPackageDetected(
        spamPackageScanResult: SpamPackageScanResult,
        alreadyKnown: Boolean
    ) {
        // Nothing to do yet
        //updateSpamPackageListView()
    }

    override fun onSpamResultPackageListUpdated() {
        // The fragment could be in the background, then the context is invalid
        context?.let {
            updateSpamPackageListView(it)
        }
    }

}