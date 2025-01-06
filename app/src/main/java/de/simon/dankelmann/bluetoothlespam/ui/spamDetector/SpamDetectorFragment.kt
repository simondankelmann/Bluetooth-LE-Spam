package de.simon.dankelmann.bluetoothlespam.ui.spamDetector

import android.bluetooth.le.ScanResult
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.ListView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.simon.dankelmann.bluetoothlespam.Adapters.AdvertisementSetCollectionExpandableListViewAdapter
import de.simon.dankelmann.bluetoothlespam.Adapters.FlipperDeviceScanResultListViewAdapter
import de.simon.dankelmann.bluetoothlespam.Adapters.SpamPackageScanResultListViewAdapter
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Enums.FlipperDeviceType
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.Models.SpamPackageScanResult
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanForegroundService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentAdvertisementBinding
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentSpamDetectorBinding
import de.simon.dankelmann.bluetoothlespam.ui.advertisement.AdvertisementViewModel

class SpamDetectorFragment : IBluetoothLeScanCallback, Fragment() {

    private val _logTag = "SpamDetectorFragment"

    private var _viewModel: SpamDetectorViewModel? = null
    private val viewModel get() = _viewModel!!

    private var _binding: FragmentSpamDetectorBinding? = null
    private val binding get() = _binding!!

    /*
    private lateinit var _flipperDevicesListView: ListView
    private lateinit var _flipperDevicesListViewAdapter: FlipperDeviceScanResultListViewAdapter

    private lateinit var _spamPackageListView: ListView
    private lateinit var _spamPackageListViewAdapter: SpamPackageScanResultListViewAdapter
    */

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

    private fun syncWithScanServices(){
        viewModel.isDetecting.postValue(AppContext.getBluetoothLeScanService().isScanning())
        updateFlipperDevicesListView()
        updateSpamPackageListView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return inflater.inflate(R.layout.fragment_spam_detector, container, false)
        Log.d(_logTag, "onCreate")
        _viewModel = ViewModelProvider(this)[SpamDetectorViewModel::class.java]
        _binding = FragmentSpamDetectorBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _flipperDevicesRecyclerView = binding.spamDetectionFlipperDevicesList
        _spamPackageRecyclerView = binding.spamDetectionSpamPackageList

        setupUi()
        setupFlipperDevicesListView()
        setupSpamPackagesListView()

        AppContext.getBluetoothLeScanService().addBluetoothLeScanServiceCallback(this)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setupUi(){
        // Views
        var toggleButton = binding.spamDetectorToggleButton
        var detectionAnimation = binding.spamDetectionAnimation

        // Listeners
        toggleButton.setOnClickListener{
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

    fun setupFlipperDevicesListView(){
        _flipperDevicesListViewAdapter = FlipperDeviceScanResultListViewAdapter(AppContext.getBluetoothLeScanService().getFlipperDevicesList())
        _flipperDevicesRecyclerView.adapter = _flipperDevicesListViewAdapter
        _flipperDevicesRecyclerView.layoutManager = LinearLayoutManager(AppContext.getActivity())
        /*
        _flipperDevicesListViewAdapter = FlipperDeviceScanResultListViewAdapter(requireActivity(), AppContext.getBluetoothLeScanService().getFlipperDevicesList())
        _flipperDevicesListView.isScrollingCacheEnabled = true
        _flipperDevicesListView.adapter = _flipperDevicesListViewAdapter*/
        /*
        listView.setOnItemClickListener(){adapterView, view, position, id ->
            // Maybe later...
        }
        */
    }

    fun setupSpamPackagesListView(){
        _spamPackageListViewAdapter = SpamPackageScanResultListViewAdapter(AppContext.getBluetoothLeScanService().getSpamPackageScanResultList())
        _spamPackageRecyclerView.adapter = _spamPackageListViewAdapter
        _spamPackageRecyclerView.layoutManager = LinearLayoutManager(AppContext.getActivity())

        /*
        _spamPackageListViewAdapter = SpamPackageScanResultListViewAdapter(requireActivity(), AppContext.getBluetoothLeScanService().getSpamPackageScanResultList())
        _spamPackageListView.isScrollingCacheEnabled = true
        _spamPackageListView.adapter = _spamPackageListViewAdapter*/
        /*
        listView.setOnItemClickListener(){adapterView, view, position, id ->
            // Maybe later...
        }
        */
    }

    fun updateFlipperDevicesListView(){
            if(_flipperDevicesListViewAdapter != null){
                var newItems = AppContext.getBluetoothLeScanService().getFlipperDevicesList()
                newItems.forEach { newFlipperDevice ->
                    var oldFlipperListIndex = -1
                    _flipperDevicesListViewAdapter.mList.forEachIndexed { index, oldFlipperDevice ->
                        if(oldFlipperDevice.address == newFlipperDevice.address){
                            oldFlipperListIndex = index
                        }
                    }

                    if(oldFlipperListIndex != -1){
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

    fun updateSpamPackageListView(){
        if(_spamPackageListViewAdapter != null){
            var newItems = AppContext.getBluetoothLeScanService().getSpamPackageScanResultList()
            newItems.forEach { newSpamPackage ->
                var oldListIndex = -1

                _spamPackageListViewAdapter.mList.forEachIndexed { index, oldSpamPackage ->
                    if(oldSpamPackage.address == newSpamPackage.address){
                        oldListIndex = index
                    }
                }

                if(oldListIndex != -1){
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

    fun onToggleButtonClicked(){
        if(viewModel.isDetecting.value == true){
            BluetoothLeScanForegroundService.stopService(AppContext.getContext())
            //AppContext.getBluetoothLeScanService().stopScanning()
            Log.d(_logTag, "Should Stop")
            viewModel.isDetecting.postValue(false)
        } else {
            BluetoothLeScanForegroundService.startService(AppContext.getContext(), "Bluetooth LE Scan Foreground Service started...")
            //AppContext.getBluetoothLeScanService().startScanning()
            viewModel.isDetecting.postValue(true)
        }
    }

    // BluetoothLeScanResult Callback Implementation
    override fun onScanResult(scanResult: ScanResult) {
        // Nothing to do yet
    }

    override fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown:Boolean) {
        // Nothing to do yet
        //updateFlipperDevicesListView()
    }

    override fun onFlipperListUpdated() {
        updateFlipperDevicesListView()
    }

    override fun onSpamResultPackageDetected(spamPackageScanResult: SpamPackageScanResult, alreadyKnown: Boolean) {
        // Nothing to do yet
        //updateSpamPackageListView()
    }

    override fun onSpamResultPackageListUpdated() {
        updateSpamPackageListView()
    }

}