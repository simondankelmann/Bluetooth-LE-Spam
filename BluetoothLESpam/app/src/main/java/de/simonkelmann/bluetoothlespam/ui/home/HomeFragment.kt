package de.simonkelmann.bluetoothlespam.ui.home

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simonkelmann.bluetoothlespam.Services.BleService
import de.simonkelmann.bluetoothlespam.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var _viewModel:HomeViewModel? = null
    private var bleService: BleService = BleService()
    private val _logTag = "HomeFragment"


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)
        _viewModel = homeViewModel

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        var startBtn: Button = binding.advertiseButton
        startBtn.setOnClickListener{view ->
            Log.d(_logTag, "OnClick");
            var macAddress = BluetoothAdapter.getDefaultAdapter().address
            _viewModel!!.setText("Started Advertising on $macAddress")
            bleService.advertise()
        }

        var stopBtn: Button = binding.stopAdvertiseButton
        stopBtn.setOnClickListener{view ->
            Log.d(_logTag, "OnClick");
            _viewModel!!.setText("Stopped Advertising")
            bleService.stopAdvertising()
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