package de.simon.dankelmann.bluetoothlespam.ui.fastPairing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import de.simon.dankelmann.bluetoothlespam.AdvertisementSetGenerators.GoogleFastPairAdvertisementSetGenerator
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Interfaces.Callbacks.IBleAdvertisementServiceCallback
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSet
import de.simon.dankelmann.bluetoothlespam.Services.AdvertisementLoopService
import de.simon.dankelmann.bluetoothlespam.Services.BluetoothLeAdvertisementService
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentFastpairingBinding

class FastPairingFragment : Fragment(), IBleAdvertisementServiceCallback {

    private var _binding: FragmentFastpairingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var _viewModel:FastPairingViewModel? = null

    private var _bluetoothLeAdvertisementService:BluetoothLeAdvertisementService = BluetoothLeAdvertisementService(AppContext.getContext().bluetoothAdapter()!!)
    private var _advertisementLoopService: AdvertisementLoopService = AdvertisementLoopService(_bluetoothLeAdvertisementService)


    private val _logTag = "FastPairingFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(FastPairingViewModel::class.java)
        _viewModel = viewModel
        _binding = FragmentFastpairingBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // setup callbacks
        _bluetoothLeAdvertisementService.addBleAdvertisementServiceCallback(this)
        _advertisementLoopService.addBleAdvertisementServiceCallback(this)

        // Add advertisement sets to the Loop Service:
        val _googleFastPairAdvertisementSetGenerator = GoogleFastPairAdvertisementSetGenerator()
        val _advertisementSets = _googleFastPairAdvertisementSetGenerator.getAdvertisementSets()
        _advertisementSets.map {
            _advertisementLoopService.addAdvertisementSet(it)
        }

        val textView: TextView = binding.statusLabelFastPairing
        viewModel.statusText.observe(viewLifecycleOwner) {
            textView.text = it
        }

        setupUi()

        return root
    }

    fun setupUi(){
        if(_viewModel != null){
            // start button
            var startBtn: Button = binding.advertiseButton
            startBtn.setOnClickListener{view ->
                _advertisementLoopService.startAdvertising()
            }

            // stop button
            var stopBtn: Button = binding.stopAdvertiseButton
            stopBtn.setOnClickListener{view ->
                _advertisementLoopService.stopAdvertising()
            }

            //animation view
            val animationView: LottieAnimationView = binding.fastPairingAnimation
            animationView.cancelAnimation()
            _viewModel!!.isTransmitting.observe(viewLifecycleOwner) {
                if(it == true){
                    animationView.playAnimation()
                } else {
                    animationView.cancelAnimation()
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAdvertisementStarted() {
        _viewModel!!.setStatusText("Started Advertising")
        _viewModel!!.isTransmitting.postValue(true)
    }
    override fun onAdvertisementStopped() {
        _viewModel!!.setStatusText("Stopped Advertising")
        _viewModel!!.isTransmitting.postValue(false)
    }

    override fun onAdvertisementSetStarted(advertisementSet: AdvertisementSet) {
        _viewModel!!.setStatusText("Advertising Set: " + advertisementSet.deviceName)
    }

}