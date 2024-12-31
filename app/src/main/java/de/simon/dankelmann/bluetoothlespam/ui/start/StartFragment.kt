package de.simon.dankelmann.bluetoothlespam.ui.start

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext.Companion.bluetoothAdapter
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Handlers.AdvertisementSetQueueHandler
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.R
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentStartBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class StartFragment : Fragment() {

    private val _logTag = "StartFragment"
    private var _viewModel: StartViewModel? = null
    private var _binding: FragmentStartBinding? = null
    private lateinit var registerForResult:ActivityResultLauncher<Intent>

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val viewModel = ViewModelProvider(this)[StartViewModel::class.java]
        _viewModel = viewModel
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _viewModel!!.appVersion.postValue(getAppVersion())
        _viewModel!!.androidVersion.postValue(android.os.Build.VERSION.RELEASE)
        _viewModel!!.sdkVersion.postValue(android.os.Build.VERSION.SDK_INT.toString())
        _viewModel!!.bluetoothSupport.postValue(getBluetoothSupportText())

        // register for bt enable callback
        registerForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                // Handle the Intent
                checkBluetoothAdapter(false)
            }
        }

        setupUi()

        checkRequiredPermissions(true)
        checkBluetoothAdapter(true)
        checkAdvertisementService()
        checkDatabase()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            return "Legacy only"
        }
    }

    fun setupUi(){

        // Loading Animation
        val loadingSpinnerLayout: View = binding.startFragmentLoadingSpinnerLayout
        _viewModel!!.isLoading.observe(viewLifecycleOwner) {
            loadingSpinnerLayout.visibility = when(it){
                true -> View.VISIBLE
                false -> View.GONE
            }
        }

        // Seeding Animation
        val seedingAnimationView: View = binding.startFragmentDatabaseCardSeedingAnimation
        val databaseImageView: View = binding.startFragmentDatabaseCardIcon
        _viewModel!!.isSeeding.observe(viewLifecycleOwner) {
            seedingAnimationView.visibility = when(it){
                true -> View.VISIBLE
                false -> View.GONE
            }

            databaseImageView.visibility = when(it){
                true -> View.GONE
                false -> View.VISIBLE
            }
        }

        // Loading Message
        val textViewLoadingMessage: TextView = binding.startFragmentLoadingSpinnerMessage
        _viewModel!!.loadingMessage.observe(viewLifecycleOwner) {
            textViewLoadingMessage.text = it
        }

        // App Version
        val textViewAppVersion: TextView = binding.infoCard.startFragmentTextViewAppVersion
        _viewModel!!.appVersion.observe(viewLifecycleOwner) {
            textViewAppVersion.text = "App Version: $it"
        }

        // Android Version
        val textViewAndroidVersion: TextView = binding.infoCard.startFragmentTextViewAndroidVersion
        _viewModel!!.androidVersion.observe(viewLifecycleOwner) {
            textViewAndroidVersion.text = "Android Version: $it"
        }

        // SDK Version
        val textViewSdkVersion: TextView = binding.infoCard.startFragmentTextViewSdkVersion
        _viewModel!!.sdkVersion.observe(viewLifecycleOwner) {
            textViewSdkVersion.text = "SDK Version: $it"
        }

        // Bluetooth Support
        val textViewBluetoothSupport: TextView = binding.infoCard.startFragmentTextViewBluetooth
        _viewModel!!.bluetoothSupport.observe(viewLifecycleOwner) {
            textViewBluetoothSupport.text = "Bluetooth: $it"
        }

        // Missing Requirements Text
        val textViewRequirementsDescription: TextView = binding.startFragmentRequirementsTextView
        val startFragmentMissingRequirementsTextView:  TextView = binding.startFragmentMissingRequirementsTextView
        _viewModel!!.missingRequirements.observe(viewLifecycleOwner) {missingRequirementsList ->
          if(missingRequirementsList.isEmpty()){
              startFragmentMissingRequirementsTextView.visibility = View.GONE
              //textViewRequirementsDescription.visibility = View.GONE
              startFragmentMissingRequirementsTextView.text = ""
              textViewRequirementsDescription.text = "All requirements are met"
          } else {
              startFragmentMissingRequirementsTextView.visibility = View.VISIBLE
              //textViewRequirementsDescription.visibility = View.VISIBLE
              textViewRequirementsDescription.text = "Missing Requirements:"
              var prepend = ""
              var missingRequirementsString = ""
              missingRequirementsList.forEach {missingRequirement ->
                  missingRequirementsString += prepend + missingRequirement
                  prepend = "\n"
              }
              startFragmentMissingRequirementsTextView.text = missingRequirementsString
          }
        }

        // Permissions CardView
        val startFragmentPermissionCardView: CardView = binding.startFragmentPermissionsCardview
        startFragmentPermissionCardView.setOnClickListener {
            checkRequiredPermissions(true)
        }

        // Permissions CardView Content
        val startFragmentPermissionCardViewContentWrapper: LinearLayout = binding.startFragmentPermissionCardViewContentWrapper
        _viewModel!!.allPermissionsGranted.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentPermissionCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_success, AppContext.getContext().theme)
            } else {
                startFragmentPermissionCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_error, AppContext.getContext().theme)
            }
        }

        // Bluetooth CardView
        val startFragmentBluetoothCardView: CardView = binding.startFragmentBluetoothCardview
        startFragmentBluetoothCardView.setOnClickListener {
            checkBluetoothAdapter(true)
        }

        // Bluetooth CardView Content
        val startFragmentBluetoothCardViewContentWrapper: LinearLayout = binding.startFragmentBluetoothCardViewContentWrapper
        _viewModel!!.bluetoothAdapterIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentBluetoothCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_success, AppContext.getContext().theme)
            } else {
                startFragmentBluetoothCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_error, AppContext.getContext().theme)
            }
        }

        // Service CardView
        val startFragmentServiceCardview: CardView = binding.startFragmentServiceCardview
        startFragmentServiceCardview.setOnClickListener {
            checkAdvertisementService()
        }

        // Service CardView Content
        val startFragmentServiceCardViewContentWrapper: LinearLayout = binding.startFragmentServiceCardViewContentWrapper
        _viewModel!!.advertisementServiceIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentServiceCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_success, AppContext.getContext().theme)
            } else {
                startFragmentServiceCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_error, AppContext.getContext().theme)
            }
        }

        // Database CardView
        val startFragmentDatabaseCardview: CardView = binding.startFragmentDatabaseCardview
        startFragmentDatabaseCardview.setOnClickListener {
            checkDatabase()
        }

        // Service CardView Content
        val startFragmentDatabaseCardViewContentWrapper: LinearLayout = binding.startFragmentDatabaseCardViewContentWrapper
        _viewModel!!.databaseIsReady.observe(viewLifecycleOwner) {
            if(it == true){
                startFragmentDatabaseCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_success, AppContext.getContext().theme)
            } else {
                startFragmentDatabaseCardViewContentWrapper.background = resources.getDrawable(R.drawable.gradient_error, AppContext.getContext().theme)
            }
        }
    }

    fun showLoadingSpinner(message:String){
        _viewModel!!.loadingMessage.postValue(message)
        _viewModel!!.isLoading.postValue(true)
    }

    fun hideLoadingSpinner(){
        _viewModel!!.isLoading.postValue(false)
    }

    fun addMissingRequirement(missingRequirement:String){
        var newList = _viewModel!!.missingRequirements.value!!
        if(!newList.contains(missingRequirement)){
            newList.add(missingRequirement)
        }
        _viewModel!!.missingRequirements.postValue(newList)
    }

    fun removeMissingRequirement(missingRequirement:String){
        var newList = _viewModel!!.missingRequirements.value!!
        newList.remove(missingRequirement)
        _viewModel!!.missingRequirements.postValue(newList)
    }

    fun checkDatabase(){
        CoroutineScope(Dispatchers.IO).launch {
            var result = false
            var database = AppDatabase.getInstance()
            if(database != null){
                removeMissingRequirement("Database is not initialized")
                if(!database.isSeeding && !database.inTransaction()){
                    removeMissingRequirement("Database is Seeding")
                    _viewModel!!.isSeeding.postValue(false)
                    var numberOfAdvertisementSetEntities = database.advertisementSetDao().getAll().count()
                    if(numberOfAdvertisementSetEntities > 0){
                        removeMissingRequirement("Database is empty")
                        result = true
                    } else {
                        addMissingRequirement("Database is empty")
                    }
                } else {
                    addMissingRequirement("Database is Seeding")
                    _viewModel!!.isSeeding.postValue(true)
                }

            } else {
                addMissingRequirement("Database is not initialized")
            }
            _viewModel!!.databaseIsReady.postValue(result)

            if(result == false){
                // Check again in a few seconds
                Executors.newSingleThreadScheduledExecutor().schedule({
                    checkDatabase()
                }, 2, TimeUnit.SECONDS)
            }
        }
    }

    fun checkBluetoothAdapter(promptIfAdapterIsDisabled:Boolean = false){
        var bluetoothIsReady = false
        // Get Bluetooth Adapter
        val bluetoothAdapter:BluetoothAdapter? = AppContext.getContext().bluetoothAdapter()
        if(bluetoothAdapter != null){
            removeMissingRequirement("Bluetooth Adapter not found")
            // Check if Bluetooth Adapter is enabled
                if(bluetoothAdapter.isEnabled){
                    removeMissingRequirement("Bluetooth is disabled")
                    bluetoothIsReady = true
                } else {
                    addMissingRequirement("Bluetooth is disabled")
                    if(promptIfAdapterIsDisabled){
                        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, AppContext.getActivity())){
                            promptEnableBluetooth(bluetoothAdapter)
                        }
                    }
                }
        } else {
            addMissingRequirement("Bluetooth Adapter not found")
        }

        _viewModel!!.bluetoothAdapterIsReady.postValue(bluetoothIsReady)
    }

    fun promptEnableBluetooth(bluetoothAdapter: BluetoothAdapter){
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        registerForResult.launch(enableBtIntent)
    }

    fun checkRequiredPermissions(promptForNotGranted:Boolean = false){
        val allPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        var notGrantedPermissions:MutableList<String> = mutableListOf()

        allPermissions.forEach {permission ->
            var missingRequirementString = "Permission " + permission.replace("android.permission.", "") + " not granted"
            val isGranted = PermissionCheck.checkPermission(permission, AppContext.getActivity(), false)

            if(isGranted){
               removeMissingRequirement(missingRequirementString)
            } else {
                notGrantedPermissions.add(permission)
                addMissingRequirement(missingRequirementString)
            }
        }

        if(notGrantedPermissions.isEmpty()){
            val backgroundLocationAccessIsGranted = checkBackgroundLocationAccessPermission(promptForNotGranted)
            var missingRequirementStringBgLocation = "Permission " + Manifest.permission.ACCESS_BACKGROUND_LOCATION.replace("android.permission.", "") + " not granted"
            if(backgroundLocationAccessIsGranted){
                removeMissingRequirement(missingRequirementStringBgLocation)
                _viewModel!!.allPermissionsGranted.postValue(true)
            } else {
                addMissingRequirement(missingRequirementStringBgLocation)
                checkBackgroundLocationAccessPermission(true)
            }
        } else {
            _viewModel!!.allPermissionsGranted.postValue(false)
            // Request Missing Permissions
            if(promptForNotGranted){
                //PermissionCheck.requireAllPermissions(AppContext.getActivity(), notGrantedPermissions.toTypedArray())
                activityResultLauncher.launch(notGrantedPermissions.toTypedArray())
            }
        }
    }

    fun checkBackgroundLocationAccessPermission(promptForNotGranted:Boolean = false):Boolean{
        val isGranted = PermissionCheck.checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, AppContext.getActivity(), false)
        if(promptForNotGranted){
            //PermissionCheck.requireAllPermissions(AppContext.getActivity(), notGrantedPermissions.toTypedArray())
            activityResultLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }
        return isGranted
    }

    private var activityResultLauncher: ActivityResultLauncher<Array<String>>
    init{
        this.activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) {result ->
            checkRequiredPermissions(false)
        }
    }

    fun checkAdvertisementService(){
        var advertisementServiceIsReady = true

        if(!AppContext.advertisementServiceIsInitialized()){
            try {
                val advertisementService = BluetoothHelpers.getAdvertisementService()
                AppContext.setAdvertisementService(advertisementService)
            } catch (e:Exception){
                addMissingRequirement("Advertisement Service not initialized")
                advertisementServiceIsReady = false
            }
        }


        if(!AppContext.bluetoothLeScanServiceIsInitialized()){
            try {
                val bluetoothLeScanService = BluetoothHelpers.getBluetoothLeScanService()
                AppContext.setBluetoothLeScanService(bluetoothLeScanService)
                //BluetoothLeScanForegroundService.startService(AppContext.getContext(), "Bluetooth LE Scan Foreground Service is running...")
            } catch (e:Exception){
                addMissingRequirement("Bluetooth LE Scan Service not initialized")
                advertisementServiceIsReady = false
            }
        }

        if(!AppContext.advertisementSetQueueHandlerIsInitialized()){
            try {
                var advertisementSetQueueHandler = AdvertisementSetQueueHandler()
                AppContext.setAdvertisementSetQueueHandler(advertisementSetQueueHandler)
            } catch (e:Exception){
                addMissingRequirement("Queue Handler not initialized")
                advertisementServiceIsReady = false
            }
        }

        _viewModel!!.advertisementServiceIsReady.postValue(advertisementServiceIsReady)
    }
}