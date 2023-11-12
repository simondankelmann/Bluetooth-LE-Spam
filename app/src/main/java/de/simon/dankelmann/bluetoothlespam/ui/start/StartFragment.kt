package de.simon.dankelmann.bluetoothlespam.ui.start

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentStartBinding


class StartFragment : Fragment() {

    private val _logTag = "StartFragment"
    private var _viewModel: StartViewModel? = null
    private var _binding: FragmentStartBinding? = null


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    companion object {
        fun newInstance() = StartFragment()
    }

    private lateinit var viewModel: StartViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val viewModel = ViewModelProvider(this)[StartViewModel::class.java]
        _viewModel = viewModel
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _viewModel!!.appVersion.postValue(getAppVersion())
        _viewModel!!.androidVersion.postValue(android.os.Build.VERSION.RELEASE)
        _viewModel!!.sdkVersion.postValue(android.os.Build.VERSION.SDK_INT.toString())
        _viewModel!!.bluetoothSupport.postValue(getBluetoothSupportText())

        setupUi()
        return root
    }

    fun getAppVersion():String{
        val manager = AppContext.getContext()!!.packageManager
        val info = manager.getPackageInfo(AppContext.getContext().packageName, 0)
        val version = info.versionName
        return version
    }

    fun getBluetoothSupportText():String{
        if(AppContext.isBluetooth5Supported()){
            return "Modern & Legacy"
        } else {
            return "Legacy Advertising only"
        }
    }

    fun setupUi(){

        // App Version
        val textViewAppVersion: TextView = binding.startFragmentTextViewAppVersion
        _viewModel!!.appVersion.observe(viewLifecycleOwner) {
            textViewAppVersion.text = "App Version: $it"
        }

        // Android Version
        val textViewAndroidVersion: TextView = binding.startFragmentTextViewAndroidVersion
        _viewModel!!.androidVersion.observe(viewLifecycleOwner) {
            textViewAndroidVersion.text = "Android Version: $it"
        }

        // SDK Version
        val textViewSdkVersion: TextView = binding.startFragmentTextViewSdkVersion
        _viewModel!!.sdkVersion.observe(viewLifecycleOwner) {
            textViewSdkVersion.text = "SDK Version: $it"
        }

        // Bluetooth Support
        val textViewBluetoothSupport: TextView = binding.startFragmentTextViewBluetooth
        _viewModel!!.bluetoothSupport.observe(viewLifecycleOwner) {
            textViewBluetoothSupport.text = "Bluetooth Version: $it"
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StartViewModel::class.java)
        // TODO: Use the ViewModel
    }

}