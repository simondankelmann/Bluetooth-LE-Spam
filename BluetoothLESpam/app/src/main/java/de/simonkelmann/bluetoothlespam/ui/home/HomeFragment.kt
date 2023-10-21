package de.simonkelmann.bluetoothlespam.ui.home

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simonkelmann.bluetoothlespam.AppContext.AppContext
import de.simonkelmann.bluetoothlespam.Services.BleService
import de.simonkelmann.bluetoothlespam.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var _viewModel:HomeViewModel? = null
    private var _bleService: BleService = BleService(AppContext.getContext().bluetoothAdapter()!!)
    private val _logTag = "HomeFragment"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    fun Context.bluetoothAdapter(): BluetoothAdapter? =
        (this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    @RequiresApi(Build.VERSION_CODES.O)
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

            var adapter = AppContext.getContext().bluetoothAdapter()
            val macAddress = adapter!!.address
            _viewModel!!.setText("Started Advertising on $macAddress")
            _bleService.startAdvertising()
        }

        var stopBtn: Button = binding.stopAdvertiseButton
        stopBtn.setOnClickListener{view ->
            _viewModel!!.setText("Stopped Advertising")
            _bleService.stopAdvertising()
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