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
import de.simon.dankelmann.bluetoothlespam.Adapters.AdvertisementSetCollectionExpandableListViewAdapter
import de.simon.dankelmann.bluetoothlespam.Adapters.FlipperDeviceScanResultListViewAdapter
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementQueueMode
import de.simon.dankelmann.bluetoothlespam.Enums.AdvertisementTarget
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBluetoothLeScanCallback
import de.simon.dankelmann.bluetoothlespam.Models.FlipperDeviceScanResult
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeScanForegroundService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentAdvertisementBinding
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentSpamDetectorBinding
import de.simon.dankelmann.bluetoothlespam.ui.advertisement.AdvertisementViewModel

class SpamDetectorFragment : IBluetoothLeScanCallback, Fragment() {

    private val _logTag = "SpamDetectorFragment"
    private var _viewModel: SpamDetectorViewModel? = null
    private var _binding: FragmentSpamDetectorBinding? = null

    private lateinit var _flipperDevicesListView: ListView
    private lateinit var _flipperDevicesListViewAdapter: FlipperDeviceScanResultListViewAdapter

    companion object {
        fun newInstance() = SpamDetectorFragment()
    }

    private lateinit var viewModel: SpamDetectorViewModel

    override fun onResume() {
        super.onResume()
        AppContext.getBluetoothLeScanService().addBluetoothLeScanServiceCallback(this)
        // SYNC THE LISTS
    }

    override fun onPause() {
        super.onPause()
        AppContext.getBluetoothLeScanService().removeBluetoothLeScanServiceCallback(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //return inflater.inflate(R.layout.fragment_spam_detector, container, false)
        Log.d(_logTag, "onCreate")
        val viewModel = ViewModelProvider(this)[SpamDetectorViewModel::class.java]
        _viewModel = viewModel
        _binding = FragmentSpamDetectorBinding.inflate(inflater, container, false)
        val root: View = _binding!!.root

        _flipperDevicesListView = _binding!!.spamDetectionFlipperDevicesList

        setupUi()
        setupFlipperDevicesListView()


        AppContext.getBluetoothLeScanService().addBluetoothLeScanServiceCallback(this)

        return root
    }

    fun setupUi(){

        // Views
        var toggleButton = _binding!!.spamDetectorToggleButton
        var detectionAnimation = _binding!!.spamDetectionAnimation

        // Listeners
        toggleButton.setOnClickListener{
            onToggleButtonClicked()
        }

        // Observers
        _viewModel!!.isDetecting.observe(viewLifecycleOwner) { isDetecting ->
            if(isDetecting){
                toggleButton.setImageDrawable(resources.getDrawable(R.drawable.pause, AppContext.getContext().theme))
                detectionAnimation.playAnimation()
            } else {
                toggleButton.setImageDrawable(resources.getDrawable(R.drawable.play_arrow, AppContext.getContext().theme))
                detectionAnimation.cancelAnimation()
                detectionAnimation.frame = 0
            }
        }
    }

    fun setupFlipperDevicesListView(){
        Log.d(_logTag, "Setting up Flipper List")
       _flipperDevicesListViewAdapter = FlipperDeviceScanResultListViewAdapter(requireActivity(), AppContext.getBluetoothLeScanService().getFlipperDevicesList() )
       _flipperDevicesListView.adapter = _flipperDevicesListViewAdapter
        /*
        listView.setOnItemClickListener(){adapterView, view, position, id ->
            // Maybe later...
        }
        */
    }



    fun onToggleButtonClicked(){
        if(_viewModel!!.isDetecting.value!!){
            BluetoothLeScanForegroundService.stopService(AppContext.getContext())
            _viewModel!!.isDetecting.postValue(false)
        } else {
            BluetoothLeScanForegroundService.startService(AppContext.getContext(), "Bluetooth LE Scan Foreground Service started...")
            _viewModel!!.isDetecting.postValue(true)
        }
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SpamDetectorViewModel::class.java)
        // TODO: Use the ViewModel
    }

    // BluetoothLeScanResult Callback Implementation
    override fun onScanResult(scanResult: ScanResult) {
        // Nothing to do yet
    }

    override fun onFlipperDeviceDetected(flipperDeviceScanResult: FlipperDeviceScanResult, alreadyKnown:Boolean) {
        if(alreadyKnown){
            Log.d(_logTag, "Flipper updated")
        } else {
            Log.d(_logTag, "New Flipper detected")
        }

        setupFlipperDevicesListView()
    }

}