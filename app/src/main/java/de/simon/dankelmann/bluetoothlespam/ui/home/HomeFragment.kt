package de.simon.dankelmann.bluetoothlespam.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.GoogleFastPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Callbacks.GoogleFastPairAdvertisingSetCallback
import de.simon.dankelmann.bluetoothlespam.Services.AdvertismentLoopService
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeAdvertisementService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _viewModel:HomeViewModel? = null
    private var _bluetoothLeAdvertisementService:BluetoothLeAdvertisementService = BluetoothLeAdvertisementService(AppContext.getContext().bluetoothAdapter()!!)
    private var _googleFastPairAdvertisementSetGenerator:GoogleFastPairAdvertisementSetGenerator = GoogleFastPairAdvertisementSetGenerator()
    private var _advertisementSets = _googleFastPairAdvertisementSetGenerator.getAdvertisementSets()

    private var _advertismentLoopService: AdvertismentLoopService = AdvertismentLoopService()
    private val _logTag = "HomeFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _viewModel = homeViewModel

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var startBtn: Button = binding.advertiseButton
        startBtn.setOnClickListener{view ->


            _advertisementSets.map {
                _advertismentLoopService.addAdvertisementSet(it)
                //_bluetoothLeAdvertisementService.startAdvertising(it)
            }
            _advertismentLoopService.startAdvertising()

            //_advertisementSets[0].callback = GoogleFastPairAdvertisingSetCallback()
            //_bluetoothLeAdvertisementService.startAdvertisingSet(_advertisementSets[0])


            _viewModel!!.setText("Started Advertising")
        }

        var stopBtn: Button = binding.stopAdvertiseButton
        stopBtn.setOnClickListener{view ->

            /*
            _advertisementSets.map {
                _bluetoothLeAdvertisementService.stopAdvertising(it)
            }*/

            _advertismentLoopService.stopAdvertising()

            _viewModel!!.setText("Stopped Advertising")
        }

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}